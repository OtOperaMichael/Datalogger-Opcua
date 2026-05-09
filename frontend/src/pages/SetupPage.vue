<template>
  <div class="setup-fullscreen">
    <div class="setup-card">
      <h2>{{ currentStep === 'database' ? 'Setup database' : 'Setup system configuration' }}</h2>

      <a-form @finish="onFinish" :model="formState" layout="vertical">
        <!-- 第一步：数据库配置 -->
        <template v-if="currentStep === 'database'">
          <a-form-item label="JDBC URL" name="jdbcUrl" required>
            <a-input v-model:value="formState.jdbcUrl"/>
          </a-form-item>
          <a-form-item label="Username" name="username" required>
            <a-input v-model:value="formState.username"/>
          </a-form-item>
          <a-form-item label="Password" name="password" required>
            <a-input-password v-model:value="formState.password"/>
          </a-form-item>
          <a-form-item label="Driver class name" name="driverClassName" required>
            <a-input v-model:value="formState.driverClassName"/>
          </a-form-item>

          <a-button type="dashed" @click="testDatabase" :loading="testing">
            Test database
          </a-button>
          <p v-if="testMessage" :class="{ 'test-error': !testSuccess }"
             style="margin-top: 8px; font-size: 14px;">
            {{ testMessage }}
          </p>
        </template>

        <!-- 第二步：系统参数配置 -->
        <template v-else>
          <a-form-item label="Password for admin account" name="adminPassword" required>
            <a-input-password
              v-model:value="formState.adminPassword"
              placeholder="please input password for admin account"
            />
          </a-form-item>
          <a-form-item label="Max server count" name="maxServerCount" required>
            <a-input-number
              v-model:value="formState.maxServerCount"
              :min="1"
              :max="32"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="Max table count" name="maxTableCount" required>
            <a-input-number
              v-model:value="formState.maxTableCount"
              :min="1"
              :max="32"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="Max table field count" name="maxTableFieldCount" required>
            <a-input-number
              v-model:value="formState.maxTableFieldCount"
              :min="10"
              :max="3600"
              style="width: 100%"
            />
          </a-form-item>
        </template>

        <!-- 按钮组 -->
        <div style="display: flex; gap: 12px; margin-top: 24px;">
          <a-button v-if="currentStep === 'system'" @click="goBack">
            ← Back
          </a-button>
          <a-button type="primary" html-type="submit" :loading="loading" style="flex: 1;">
            {{ currentStep === 'database' ? 'Next' : 'Complete and start app' }}
          </a-button>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {message} from 'ant-design-vue'
import request from "@/utils/request.ts"

const currentStep = ref<'database' | 'system'>('database')
const loading = ref(false)
const testing = ref(false)
const testMessage = ref('')
const testSuccess = ref(false)

const formState = ref({
  // 数据库配置
  jdbcUrl: 'jdbc:postgresql://datalogger-opcua-database:5432/datalogger',
  username: 'postgres',
  password: '123456',
  driverClassName: 'org.postgresql.Driver',
  // 系统参数
  adminPassword: 'admin',
  maxServerCount: 20,
  maxTableCount: 20,
  maxTableFieldCount: 20
})

// ===== 测试数据库连接 =====
const testDatabase = async () => {
  testing.value = true
  testMessage.value = ''
  try {
    let {data} = await request.post('common/testDatabaseConnection', {
      url: formState.value.jdbcUrl,
      username: formState.value.username,
      password: formState.value.password,
      driverClassName: formState.value.driverClassName
    })
    if (data.code === 200) {
      testSuccess.value = true
      testMessage.value = ' Database connection ok！'
      message.success('Database test passed')
    } else {
      testSuccess.value = false
      testMessage.value = 'Connection failed'
    }
  } catch (error: any) {
    testSuccess.value = false
    const msg = error.response?.data?.message || error.message || 'unknown error'
    testMessage.value = `${msg}`
    message.error('Database test failed: ' + msg)
  } finally {
    testing.value = false
  }
}

// ===== 返回上一步 =====
const goBack = () => {
  currentStep.value = 'database'
  testMessage.value = ''
}

// ===== 提交配置 =====
const onFinish = async () => {
  if (currentStep.value === 'database') {
    // 第一步：先测试连接
    await testDatabase()
    if (!testSuccess.value) {
      message.warning('please test the database connection first')
      return
    }

    // 保存数据库配置
    loading.value = true
    try {
      let {data} = await request.post('common/saveDatabaseConfig', {
        url: formState.value.jdbcUrl,
        username: formState.value.username,
        password: formState.value.password,
        driverClassName: formState.value.driverClassName
      })

      if (data.code === 200) {
        message.success('Database config saved！')
        currentStep.value = 'system'
      } else {
        message.error('Save failed：' + data.message)
      }
    } catch (error: any) {
      message.error('Save failed：' + (error.response?.data?.message || error.message))
    } finally {
      loading.value = false
    }
  } else {
    // 第二步：保存系统参数配置
    loading.value = true
    try {
      let {data} = await request.post('common/saveSystemConfig', {
        adminPassword: formState.value.adminPassword,
        maxServerCount: formState.value.maxServerCount,
        maxTableCount: formState.value.maxTableCount,
        maxTableFieldCount: formState.value.maxTableFieldCount
      })

      if (data.code === 200) {
        message.success('System config saved！')
        setTimeout(() => {
          window.location.href = '/'
        }, 1500)
      } else {
        message.error('Save failed：' + data.message)
      }
    } catch (error: any) {
      message.error('Save failed：' + (error.response?.data?.message || error.message))
    } finally {
      loading.value = false
    }
  }
}
</script>

<style scoped>
.setup-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.setup-card {
  width: 500px;
  padding: 32px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.test-error {
  color: #ff4d4f;
}

.setup-card h2 {
  text-align: center;
  margin-bottom: 24px;
}
</style>
