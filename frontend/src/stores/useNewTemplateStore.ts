import {defineStore} from 'pinia'
import {type TableInterface, TagType, type TemplateInterface} from "@/types/template.ts";



// //  提取“非 templateList”的初始状态（作为常量）
// const getDefaultTemplate = () => ({
//   templateName: "",
//   sampleInterval: 500,
//   hostCpuSlot: "1,0",
//   tableList: Array.from({length: 10}, () => ({
//     tableName: "",
//     DBType: DBType.Mysql,
//     tagAddr: "",
//     tagType: TagType.BOOL,
//     nameList: Array(16).fill("")
//   }))
// });

export const useNewTemplateStore = defineStore("newTemplateStore", {
  state: () => ({
    id: "",
    name: "",
    sampleInterval: 500,
    hostCpuSlot: "1,0",
    tableList: Array.from({length: 1}, () => ({
      name: "",
      tagAddr: "",
      tagType: TagType.Int,
      tagNameList: Array(10).fill("")
    })),
    createNew: false
  }),

  actions: {
    // reset() {
    //   // 新增：仅重置除 templateList 外的字段
    //   const defalutData = getDefaultTemplate();
    //   Object.assign(this, defalutData);
    //   // 注意：Object.assign 不会影响 templateList，因为它不在 resetData 中
    // },

    /**
     * 发送给后端转换用
     */
    toJSON() {
      return {
        id: this.id,
        name: this.name,
        sampleInterval: this.sampleInterval,
        hostCpuSlot: this.hostCpuSlot,
        tableList: this.tableList
      };
    },

    /**
     *用templateList中选中的赋值
     * @param data
     */
    loadTemplate(data: TemplateInterface) {
      // Object.assign(this, data);
      this.id = data.id;
      this.name = data.name;
      this.sampleInterval = data.sampleInterval;
      this.hostCpuSlot = data.hostCpuSlot;
      this.tableList = [...data.tableList]; // 浅拷贝避免引用共享
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

    // 校验 hostCpuSlot 格式必须为 "数字,数字"
    validateHostCpuSlot() {
      const value = this.hostCpuSlot

      // 1. 必须是字符串
      if (typeof value !== 'string') {
        return {valid: false, message: 'Host CPU slot must be a string in format "x,x"'}
      }

      // 2. 使用正则匹配：严格两个非负整数，中间一个逗号，无空格
      const match = value.match(/^(\d+),(\d+)$/)
      if (!match) {
        return {
          valid: false,
          message: 'Host CPU slot must be in format "x,x" where x is a non-negative integer (e.g., "1,0")'
        }
      }

      return {valid: true}
    },

    // 校验单个 table
    validateSingleTable(table: TableInterface, index: number) {
      const errors = []
      const tn = table.name
      const ta = table.tagAddr
      const nl = table.tagNameList

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

      // 2. tagAddr 不能为空
      if (ta === '') {
        errors.push(`Table ${index + 1}: Tag address is required`)
      }

      // 3. nameList 所有元素都不能为空
      for (let i = 0; i < nl.length; i++) {
        const tag = nl[i] as string

        if (tag === '') {
          errors.push(`Table ${index + 1}, Tag ${i + 1}: Tag name is required (cannot be empty)`)
        } else {
          // 校验 tag 格式
          if (/\s/.test(tag)) {
            errors.push(`Table ${index + 1}, Tag "${tag}": Cannot contain spaces or whitespace`)
          }
          if (!/^[a-z][a-z0-9_-]*$/.test(tag)) {
            errors.push(`Table ${index + 1}, Tag "${tag}": Must start with a lowercase letter and contain only lowercase letters, digits, underscores (_), or hyphens (-)`)
          }
        }
      }

      // 4. nameList 所有元素不能重复
      const seenTags = new Set()
      for (let i = 0; i < nl.length; i++) {
        const tag = nl[i] as string
        if (tag !== '') {
          if (seenTags.has(tag)) {
            errors.push(`Table ${index + 1}: Duplicate tag name "${tag}" in nameList`)
          } else {
            seenTags.add(tag)
          }
        }
      }

      return errors
    },

    // 校验整个 tableList
    validateTableList() {
      const allErrors = []

      // 1. 先校验每个 table 的内部规则
      for (let i = 0; i < this.tableList.length; i++) {
        const table = this.tableList[i] as TableInterface
        const errors = this.validateSingleTable(table, i)
        allErrors.push(...errors)
      }

      // 2. 跨表格 tableName 唯一性校验（仅非空）
      const seenTableNames = new Set()
      for (let i = 0; i < this.tableList.length; i++) {
        const tableName = this.tableList[i]?.name
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
      const cpuSlotCheck = this.validateHostCpuSlot()
      if (!cpuSlotCheck.valid) {
        return {valid: false, field: 'hostCpuSlot', message: cpuSlotCheck.message}
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

      const tableCheck = this.validateTableList()
      if (!tableCheck.valid) {
        return {valid: false, field: 'tableList', message: tableCheck.message}
      }

      return {valid: true}

    },

    addTable() {
      if (this.tableList.length >= 100) {
        return false;
      }
      this.tableList.push({
        name: "",
        tagAddr: "",
        tagType: TagType.BOOL,
        tagNameList: Array(10).fill("")
      });
      return true;
    },

    removeTable(index: number) {
      if (this.tableList.length <= 1) {
        return false;
      }
      this.tableList.splice(index, 1);
      return true;
    },

    addTagName(tableIndex: number) {
      const table = this.tableList[tableIndex];
      if (!table || table.tagNameList.length >= 20) {
        return false;
      }
      table.tagNameList.push("");
      return true;
    },

    removeTagName(tableIndex: number, tagNameIndex: number) {
      const table = this.tableList[tableIndex];
      if (!table || table.tagNameList.length <= 1) {
        return false;
      }
      table.tagNameList.splice(tagNameIndex, 1);
      return true;
    }
  }


});

