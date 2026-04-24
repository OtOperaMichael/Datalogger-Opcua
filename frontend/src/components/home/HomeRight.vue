<script setup lang="ts">
import {useSelectedServerStore} from "@/stores/useSelectedServerStore.ts";
import {useTemplateListStore} from "@/stores/useTemplateListStore.ts";
import type {
  TemplateInterface,
  CustomTableInterface,
  CustomModuleInterface,
  AlarmModuleInterface,
  CommModuleInterface,
  ModuleType
} from "@/types/template.ts";
import {ref, computed, watch} from 'vue';
import {message} from 'ant-design-vue';
import request from "@/utils/request.ts";
import * as XLSX from 'xlsx';
import dayjs from 'dayjs';
import type {Dayjs} from 'dayjs';
import {SearchOutlined, DownloadOutlined} from '@ant-design/icons-vue';

const serverStore = useSelectedServerStore();
const templateListStore = useTemplateListStore();

// Get template for current server
const template = computed(() => {
  return templateListStore.getTemplateByName(serverStore.templateName);
});

// Common module interface for all module types
interface BaseModuleInterface {
  enable: boolean;
  tableList: Array<{
    name: string;
    sampleInterval: number;
    nodeGroupType: any;
    nodeList: any[];
  }>;
}

// Module name mapping (string to enum and display label)
const moduleConfig = [
  { value: 'custom', label: 'Custom Module', key: 'custom' as keyof TemplateInterface },
  { value: 'alarm', label: 'Alarm Module', key: 'alarm' as keyof TemplateInterface },
  { value: 'communication', label: 'Communication Module', key: 'communication' as keyof TemplateInterface }
];

// Query form state
const queryForm = ref({
  selectedModule: '' as string,
  selectedTable: ''as string,
  startTime: null as Date | null,
  endTime: null as Date | null
});

// Query results
const queryResults = ref<any[]>([]);
const loading = ref(false);
const exporting = ref(false);

// Computed property for available modules (only enabled modules)
const availableModules = computed(() => {
  if (!template.value) return [];

  return moduleConfig.filter(config => {
    const module = template.value?.[config.key] as BaseModuleInterface | undefined;
    return module?.enable;
  }).map(config => ({
    value: config.value,
    label: config.label
  }));
});

// Computed property for available tables in selected module
const availableTables = computed(() => {
  if (!template.value || !queryForm.value.selectedModule) return [];

  const config = moduleConfig.find(c => c.value === queryForm.value.selectedModule);
  if (!config) return [];

  // Handle different module types with different table lists
  if (queryForm.value.selectedModule === 'custom') {
    // Custom module: get tables from template
    const module = template.value?.[config.key] as CustomModuleInterface | undefined;
    if (!module?.tableList) return [];
    return module.tableList.filter((table: CustomTableInterface) => table.name && table.name.trim() !== '');
  } else if (queryForm.value.selectedModule === 'alarm') {
    // Alarm module: fixed table list
    return [
      { name: 'raw' },
      { name: 'top times' },
      { name: 'top duration' }
    ];
  } else if (queryForm.value.selectedModule === 'communication') {
    // Communication module: only raw table
    return [
      { name: 'raw' }
    ];
  }

  return [];
});

// Reset form when server changes
watch(() => serverStore.name, () => {
  resetQuery();
}, {immediate: true});

// Reset table selection when module changes
watch(() => queryForm.value.selectedModule, () => {
  queryForm.value.selectedTable = '';
  queryResults.value = [];
});

function resetQuery() {
  queryForm.value.selectedModule = '';
  queryForm.value.selectedTable = '';
  queryForm.value.startTime = null;
  queryForm.value.endTime = null;
  queryResults.value = [];
}

// Validate query parameters
function validateQuery() {
  if (!queryForm.value.selectedModule) {
    message.error('Please select a module');
    return false;
  }

  if (!queryForm.value.selectedTable) {
    message.error('Please select a table');
    return false;
  }

  if (!queryForm.value.startTime || !queryForm.value.endTime) {
    message.error('Please select both start and end time');
    return false;
  }

  // Check if time range exceeds 1 month
  const start = new Date(queryForm.value.startTime);
  const end = new Date(queryForm.value.endTime);
  const diffTime = Math.abs(end.getTime() - start.getTime());
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  if (diffDays > 31) {
    message.error('Time range cannot exceed 1 month');
    return false;
  }

  if (start >= end) {
    message.error('Start time must be before end time');
    return false;
  }

  return true;
}

// Execute query
async function executeQuery() {
  if (!validateQuery()) return;

  loading.value = true;
  try {
    // Find the selected table to determine database type
    const selectedTable = availableTables.value.find((t: any) => t.name === queryForm.value.selectedTable);
    if (!selectedTable) {
      throw new Error('Selected table not found');
    }

    const payload = {
      serverName: serverStore.name,
      moduleName: queryForm.value.selectedModule,
      tableName: queryForm.value.selectedTable,
      startTime: formatDate(queryForm.value.startTime),
      endTime: formatDate(queryForm.value.endTime),
    };

    const response = await request.post('instance/query', payload);

    if (response.data.code === 200) {
      queryResults.value = response.data.data.results || [];
      // console.log('Query results:', queryResults.value)
      message.success(`Query completed. Found ${queryResults.value.length} records.`);
    } else {
      throw new Error(response.data.message || 'Query failed');
    }
  } catch (error: any) {
    console.error('Query error:', error);
    message.error(error.message || 'Failed to execute query');
    queryResults.value = [];
  } finally {
    loading.value = false;
  }
}

// Export to Excel
async function exportToExcel() {
  if (queryResults.value.length === 0) {
    message.warning('No data to export');
    return;
  }

  exporting.value = true;
  try {
    // Create worksheet
    const ws = XLSX.utils.json_to_sheet(queryResults.value);

    // Create workbook
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Query Results');

    // Generate filename
    const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-');
    const filename = `${serverStore.name}_${queryForm.value.selectedModule}_${queryForm.value.selectedTable}_${timestamp}.xlsx`;

    // Export file
    XLSX.writeFile(wb, filename);

    message.success('Export completed successfully');
  } catch (error) {
    console.error('Export error:', error);
    message.error('Failed to export data');
  } finally {
    exporting.value = false;
  }
}

// 直接使用 YYYY-MM-DD HH:mm:ss 格式
const formatDate = (date: Date | null): string | null => {
  if (!date) return null;

  // 安全转换为Date对象
  const dateObj = new Date(date);

  // 检查是否为有效日期
  if (isNaN(dateObj.getTime())) {
    console.error('Invalid date input:', date);
    return null;
  }
  const year = dateObj.getFullYear();
  const month = String(dateObj.getMonth() + 1).padStart(2, '0');
  const day = String(dateObj.getDate()).padStart(2, '0');
  const hour = String(dateObj.getHours()).padStart(2, '0');
  const minute = String(dateObj.getMinutes()).padStart(2, '0');
  const second = String(dateObj.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
};

// Handle date range change
const handleDateChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
  if (dates && dates[0] && dates[1]) {
    queryForm.value.startTime = dates[0].toDate();
    queryForm.value.endTime = dates[1].toDate();
  } else {
    queryForm.value.startTime = null;
    queryForm.value.endTime = null;
  }
};

// 添加分页配置
const pageSize = ref(20);
const paginationConfig = computed(() => ({
  pageSize: pageSize.value,
  showSizeChanger: true,
  onShowSizeChange: (current: number, size: number) => {
    pageSize.value = size;
  },
  showTotal: (total: number) => `Total ${total} items`
}));

// 添加辅助函数
function isDateKey(key: string | number): boolean {
  const keyStr = String(key).toLowerCase();

  // 排除统计字段（这些字段虽然包含 'time' 但不是时间类型）
  const excludeKeywords = ['trigger_times', 'total_duration', 'avg_duration', 'max_duration'];
  for (const exclude of excludeKeywords) {
    if (keyStr.includes(exclude)) {
      return false;
    }
  }

  // 只将明确的时间字段识别为日期
  return keyStr === 'time' ||
    keyStr === 'start_time' ||
    keyStr === 'end_time' ||
    keyStr.includes('_timestamp') ||
    (keyStr.includes('time') && !keyStr.includes('_times'));
}

</script>

<template>
  <div id="HomeRightFrame">

    <a-card title="Data Query" :bordered="false">
      <!-- 水平布局容器 - 工具栏 -->
      <a-flex
        align="center"
        justify="space-between"
        class="query-toolbar"
      >
        <!-- 左侧：模块选择、表选择和时间范围 -->
        <a-flex align="center" gap="12">
          <!-- 模块选择 -->
          <a-select
            v-model:value="queryForm.selectedModule"
            placeholder="Choose a module"
            :options="availableModules"
            allow-clear
            style="min-width: 180px; width: 200px;"
          />

          <!-- 表选择 -->
          <a-select
            v-model:value="queryForm.selectedTable"
            placeholder="Choose a table"
            :options="availableTables.map((table: any) => ({ value: table.name, label: table.name }))"
            allow-clear
            :disabled="!queryForm.selectedModule"
            style="min-width: 180px; width: 200px;"
          />

          <!-- 时间范围选择器 -->
          <a-range-picker
            :value="queryForm.startTime && queryForm.endTime ?
              [dayjs(queryForm.startTime), dayjs(queryForm.endTime)] : null"
            @change="handleDateChange"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            :placeholder="['Start Time', 'End Time']"
            style="min-width: 300px; width: 360px;"
          />
        </a-flex>

        <!-- 右侧：操作按钮 -->
        <a-space>
          <a-button
            type="primary"
            @click="executeQuery"
            :loading="loading"
            :disabled="!queryForm.selectedModule || !queryForm.selectedTable || !queryForm.startTime || !queryForm.endTime"
          >
            <template #icon>
              <SearchOutlined/>
            </template>
            Query Data
          </a-button>

          <a-button
            @click="resetQuery"
            :disabled="loading"
          >
            Reset
          </a-button>

          <a-button
            @click="exportToExcel"
            :loading="exporting"
            :disabled="queryResults.length === 0 || loading"
          >
            <template #icon>
              <DownloadOutlined/>
            </template>
            Export to Excel
          </a-button>
        </a-space>
      </a-flex>

      <!-- 查询结果区域 - 保持原有结构 -->
      <div v-if="queryResults.length > 0" class="results-section">
        <div class="results-header">
          <span class="query-info">
            Module: <strong>{{ queryForm.selectedModule }}</strong> |
            Table: <strong>{{ queryForm.selectedTable }}</strong> |
            Time Range: <strong>{{ formatDate(queryForm.startTime) }}</strong> to
            <strong>{{ formatDate(queryForm.endTime) }}</strong> |
            Total Records: <strong>{{ queryResults.length }}</strong>
          </span>
        </div>

        <div class="table-container">
          <a-table
            :dataSource="queryResults"
            :pagination="paginationConfig"
            :scroll="{ x: 800, y: 1000 }"
            size="small"
            bordered
          >
            <a-table-column
              v-for="(value, key) in queryResults[0]"
              :key="key"
              :data-index="key"
              :title="key"
              :sorter="(a: Record<string, any>, b: Record<string, any>) => {
                if (typeof a[key] === 'number' && typeof b[key] === 'number') {
                  return a[key] - b[key];
                }
                const aValue = a[key] ?? '';
                const bValue = b[key] ?? '';
                return String(aValue).localeCompare(String(bValue));
              }"
            >
              <template #default="{ record }">
                <span v-if="isDateKey(key)">
                  {{ formatDate(record[key]) }}
                </span>
                <span v-else>
                  {{ record[key] }}
                </span>
              </template>
            </a-table-column>
          </a-table>
        </div>
      </div>

      <!-- 无结果提示 -->
      <a-empty
        v-else-if="!loading && queryForm.selectedTable"
        description="No data found for the selected criteria"
      />
    </a-card>
  </div>
</template>

<style scoped>
#HomeRightFrame {
  background-color: lightgrey;
  padding: 20px;
  border-radius: 8px;
  margin: 10px;
  min-height: 500px;
}

.results-section {
  margin-top: 20px;
}

.results-header {
  margin-bottom: 15px;
  padding: 10px;
  background-color: #fafafa;
  border-radius: 4px;
}

.query-info {
  font-size: 14px;
  color: #666;
}

.table-container {
  background-color: white;
  border-radius: 4px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #f0f0f0;
  font-weight: 600;
}

:deep(.ant-table-tbody > tr:hover) {
  background-color: #f9f9f9;
}
</style>
