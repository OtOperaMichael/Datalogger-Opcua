import {defineStore} from 'pinia'
import {
  NodeType, DataType,
  type CustomNodeInterface,
  type CustomTableInterface,
  type CustomModuleInterface,
  type TemplateInterface,
} from "@/types/template.ts";


export const useNewTemplateStore = defineStore("newTemplateStore", {
  state: () => ({
    id: "",
    name: "",
    sampleInterval: 1000,
    port: "4840",
    postfix: "",
    custom: {
      enable: true,
      tableList: Array.from({length: 1}, () => ({
        name: "",
        nodeType: NodeType.SCALAR,
        nodeList: Array.from({length: 10}, () => ({
          name: "",
          nodeId: "",
          dataType: DataType.INT
        }))
      })) as CustomTableInterface[]
    } as CustomModuleInterface,
    alarm: {
      enable: false,
      tableList: [] as CustomTableInterface[]
    } as CustomModuleInterface,
    communication: {
      enable: false,
      tableList: [] as CustomTableInterface[]
    } as CustomModuleInterface,
    createNew: false
  }),

  actions: {
    /**
     * 发送给后端转换用
     */
    toJSON() {
      return {
        id: this.id,
        name: this.name,
        sampleInterval: this.sampleInterval,
        port: this.port,
        postfix: this.postfix,
        custom: JSON.parse(JSON.stringify(this.custom)),
        alarm: JSON.parse(JSON.stringify(this.alarm)),
        communication: JSON.parse(JSON.stringify(this.communication))
      };
    },

    /**
     *用templateList中选中的赋值
     * @param data
     */
    loadTemplate(data: TemplateInterface) {
      this.id = data.id;
      this.name = data.name;
      this.sampleInterval = data.sampleInterval;
      this.port = data.port;
      this.postfix = data.postfix || "";
      this.custom = data.custom ? JSON.parse(JSON.stringify(data.custom)) : this.custom;
      this.alarm = data.alarm ? JSON.parse(JSON.stringify(data.alarm)) : this.alarm;
      this.communication = data.communication ? JSON.parse(JSON.stringify(data.communication)) : this.communication;
    },

    // 校验 templateName
    validateTemplateName(): { valid: boolean; message?: string } {
      const name = this.name
      if (!name) {
        return {valid: false, message: 'Template name is required'}
      }
      if (/\s/.test(name)) {
        return {valid: false, message: 'Template name cannot contain spaces or whitespace'}
      }
      if (!/^[a-z][a-z0-9_-]*$/.test(name)) {
        return {
          valid: false,
          message: 'Template name must start with a lowercase letter and can only contain lowercase letters, digits, underscores (_), or hyphens (-)'
        }
      }
      return {valid: true}
    },

    // 校验 sampleInterval
    validateSampleInterval() {
      const value = this.sampleInterval

      // 检查是否为数字
      if (isNaN(value)) {
        return {valid: false, message: 'Sample interval must be a number'}
      }

      // 检查是否为整数
      if (!Number.isInteger(value)) {
        return {valid: false, message: 'Sample interval must be an integer'}
      }

      // 检查范围 [100, 1000]
      if (value < 100 || value > 1000) {
        return {valid: false, message: 'Sample interval must be between 100 and 1000'}
      }

      return {valid: true}
    },

    // 校验 port 格式必须为纯数字，不能以 0 开头
    validatePort() {
      const value = this.port

      // 1. 必须是字符串且不能为空
      if (typeof value !== 'string' || !value) {
        return {valid: false, message: 'Port is required'}
      }

      // 2. 必须是纯数字，不能以 0 开头（除非就是 "0"）
      // 正则解释：^[1-9]\d*$ 表示以 1-9 开头，后面跟任意数字；^0$ 表示单独的 0
      const match = value.match(/^[1-9]\d*$/)
      if (!match) {
        return {
          valid: false,
          message: 'Port must be a number without leading zeros (e.g., "44818", not "044818")'
        }
      }

      // 3. 检查端口范围 1-65535
      const portNum = parseInt(value, 10)
      if (portNum < 1 || portNum > 65535) {
        return {
          valid: false,
          message: 'Port must be between 1 and 65535'
        }
      }

      return {valid: true}
    },

    // 校验 postfix：可以为空，如果不为空则中间不能有空格
    validatePostfix() {
      const value = this.postfix

      // 如果为空，直接返回有效
      if (!value || value === '') {
        return {valid: true}
      }

      // 如果不为空，检查不能包含空格
      if (/\s/.test(value)) {
        return {
          valid: false,
          message: 'Postfix cannot contain spaces or whitespace'
        }
      }

      return {valid: true}
    },

    // 校验单个 table, custom module
    validateSingleCustomTable(table: CustomTableInterface, index: number) {
      const errors = []
      const tn = table.name
      const nt = table.nodeType
      const nl = table.nodeList

      // 1. tableName 不能为空
      if (tn === '') {
        errors.push(`Table ${index + 1}: Table name is required`)
      } else {
        // 只有在有值时才校验格式
        if (/\s/.test(tn)) {
          errors.push(`Table ${index + 1}: Table name cannot contain spaces or whitespace`)
        }
        if (!/^[a-z][a-z0-9_-]*$/.test(tn)) {
          errors.push(`Table ${index + 1}: Table name must start with a lowercase letter and contain only lowercase letters, digits, underscores (_), or hyphens (-)`)
        }
      }

      // 2. nodeType 必须是 SCALAR 或 ARRAY
      if (nt !== NodeType.SCALAR && nt !== NodeType.ARRAY) {
        errors.push(`Table ${index + 1}: Node type must be SCALAR or ARRAY`)
      }

      // 3. nodeList 不能为空
      if (!nl || nl.length === 0) {
        errors.push(`Table ${index + 1}: Node list is required and cannot be empty`)
      } else {
        // 校验每个 node
        for (let i = 0; i < nl.length; i++) {
          const node = nl[i]

          // 安全检查：node 不能为 undefined
          if (!node) {
            errors.push(`Table ${index + 1}, Node ${i + 1}: Node is undefined`)
            continue
          }

          // nodeName 不能为空
          if (!node.name || node.name === '') {
            errors.push(`Table ${index + 1}, Node ${i + 1}: Node name is required`)
          } else {
            // 校验 nodeName 格式
            if (/\s/.test(node.name)) {
              errors.push(`Table ${index + 1}, Node "${node.name}": Cannot contain spaces or whitespace`)
            }
            if (!/^[a-z][a-z0-9_-]*$/.test(node.name)) {
              errors.push(`Table ${index + 1}, Node "${node.name}": Must start with a lowercase letter and contain only lowercase letters, digits, underscores (_), or hyphens (-)`)
            }
          }

          // nodeId 不能为空
          if (!node.nodeId || node.nodeId === '') {
            errors.push(`Table ${index + 1}, Node ${i + 1}: NodeId is required`)
          }

          // dataType 必须有效
          if (node.dataType === undefined || node.dataType === null) {
            errors.push(`Table ${index + 1}, Node ${i + 1}: Data type is required`)
          } else if (node.dataType !== DataType.BOOL &&
                     node.dataType !== DataType.INT &&
                     node.dataType !== DataType.DOUBLE &&
                     node.dataType !== DataType.STRING) {
            errors.push(`Table ${index + 1}, Node ${i + 1}: Invalid data type. Must be BOOL, INT, DOUBLE, or STRING`)
          }
        }

        // 4. nodeList 所有 nodeName 不能重复
        const seenNames = new Set()
        for (let i = 0; i < nl.length; i++) {
          const node = nl[i]

          // 安全检查
          if (!node) continue

          const nodeName = node.name
          if (nodeName) {
            if (seenNames.has(nodeName)) {
              errors.push(`Table ${index + 1}: Duplicate node name "${nodeName}" in nodeList`)
            } else {
              seenNames.add(nodeName)
            }
          }
        }
      }

      return errors
    },

    // 校验整个 custom module
    validateCustomModule() {
      // 如果 custom module 未启用，直接返回有效
      if (!this.custom.enable) {
        return {valid: true}
      }

      const allErrors = []

      // 1. 先校验每个 table 的内部规则
      for (let i = 0; i < this.custom.tableList.length; i++) {
        const table = this.custom.tableList[i]
        if (table) {
          const errors = this.validateSingleCustomTable(table, i)
          allErrors.push(...errors)
        }
      }

      // 2. 跨表格 tableName 唯一性校验（仅非空）
      const seenTableNames = new Set()
      for (let i = 0; i < this.custom.tableList.length; i++) {
        const tableName = this.custom.tableList[i]?.name
        if (tableName !== '') {
          if (seenTableNames.has(tableName)) {
            allErrors.push(`Table name "${tableName}" is duplicated in Table ${i + 1}`)
          } else {
            seenTableNames.add(tableName)
          }
        }
      }

      if (allErrors.length > 0) {
        return {valid: false, message: allErrors[0]} // 或返回 allErrors.join('\n')
      }

      return {valid: true}
    },

    //校验整个template表格
    validateTemplate(): { valid: boolean; field?: string; message?: string } {

      //1. 校验template name
      const nameCheck = this.validateTemplateName()
      if (!nameCheck.valid) {
        return {valid: false, field: 'templateName', message: nameCheck.message}
      }

      //2. sample interval不能小于100 大于1000
      const intervalCheck = this.validateSampleInterval()
      if (!intervalCheck.valid) {
        return {valid: false, field: 'sampleInterval', message: intervalCheck.message}
      }

      //3. hostCpuSlot 格式必须为 “x,x” x为数字
      const portCheck = this.validatePort()
      if (!portCheck.valid) {
        return {valid: false, field: 'hostCpuSlot', message: portCheck.message}
      }

      //4. postfix 校验
      const postfixCheck = this.validatePostfix()
      if (!postfixCheck.valid) {
        return {valid: false, field: 'postfix', message: postfixCheck.message}
      }

      /** 4.tableList 的 table校验
       * 每个table的 table name以字母开头，不能包含空格, 或为空
       *
       * 每个table的 tagaddr 以[0] 结束，或为空
       *
       * 每个table的 taglist 的所有元素 必须以字母头，不能包含空格， 或为空
       *
       * 每个table的 taglist 的所有元素 不能重复
       *
       * 每个table的 taglist 数组的开始元素 和非空元素之间不能有空串
       *
       * 每个table的 的name 和 tagAddr 和 taglist必须同时有值 或同时无值
       *
       * 不同table的name如果非空 则不能重复

       * 至少第一个table是有效的 新增！
       */

      const customModuleCheck = this.validateCustomModule()
      if (!customModuleCheck.valid) {
        return {valid: false, field: 'tableList', message: customModuleCheck.message}
      }

      return {valid: true}

    },

    addTable(moduleName: 'custom' | 'alarm' | 'communication') {
      if (moduleName === 'custom') {
        const module = this.custom;
        if (!module || module.tableList.length >= 100) {
          return false;
        }

        module.tableList.push({
          name: "",
          nodeType: NodeType.SCALAR,
          nodeList: Array.from({length: 10}, () => ({
            name: "",
            nodeId: "",
            dataType: DataType.INT
          }))
        });
        return true;
      } else if (moduleName === 'alarm') {
        // TODO: Implement alarm module table addition
        // Alarm module may have different table structure
        return false;
      } else if (moduleName === 'communication') {
        // TODO: Implement communication module table addition
        // Communication module may have different table structure
        return false;
      }
      return false;
    },

    removeTable(moduleName: 'custom' | 'alarm' | 'communication', index: number) {
      if (moduleName === 'custom') {
        const module = this.custom;
        if (!module || module.tableList.length <= 1) {
          return false;
        }

        module.tableList.splice(index, 1);
        return true;
      } else if (moduleName === 'alarm') {
        // TODO: Implement alarm module table removal
        // Alarm module may have different table structure and removal logic
        return false;
      } else if (moduleName === 'communication') {
        // TODO: Implement communication module table removal
        // Communication module may have different table structure and removal logic
        return false;
      }
      return false;
    },

    addTagName(moduleName: 'custom' | 'alarm' | 'communication', tableIndex: number) {
      if (moduleName === 'custom') {
        const module = this.custom;
        if (!module) {
          return false;
        }

        const table = module.tableList[tableIndex];
        if (!table || table.nodeList.length >= 100) {
          return false;
        }

        table.nodeList.push({
          name: "",
          nodeId: "",
          dataType: DataType.INT
        });
        return true;
      } else if (moduleName === 'alarm') {
        // TODO: Implement alarm module tag/node addition
        // Alarm module may have different node structure
        return false;
      } else if (moduleName === 'communication') {
        // TODO: Implement communication module tag/node addition
        // Communication module may have different node structure
        return false;
      }
      return false;
    },

    removeTagName(moduleName: 'custom' | 'alarm' | 'communication', tableIndex: number, tagNameIndex: number) {
      if (moduleName === 'custom') {
        const module = this.custom;
        if (!module) {
          return false;
        }

        const table = module.tableList[tableIndex];
        if (!table || table.nodeList.length <= 1) {
          return false;
        }

        table.nodeList.splice(tagNameIndex, 1);
        return true;
      } else if (moduleName === 'alarm') {
        // TODO: Implement alarm module tag/node removal
        // Alarm module may have different node structure and removal logic
        return false;
      } else if (moduleName === 'communication') {
        // TODO: Implement communication module tag/node removal
        // Communication module may have different node structure and removal logic
        return false;
      }
      return false;
    }
  }


});

