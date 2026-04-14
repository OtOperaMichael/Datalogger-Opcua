import {defineStore} from 'pinia'
import {TagType, type TemplateInterface} from "@/types/template.ts";
import request from "@/utils/request.ts";
import {message} from "ant-design-vue";

export const useTemplateListStore = defineStore("templateListStore", {
  state: () => ({
    // templateList: [] as TemplateInterface[]
    templateList: [
      {
        id: "1undefined template",
        name: "undefined template",
        sampleInterval: 499,
        hostCpuSlot: "1,0",
        tableList: Array.from({length: 10}, () => ({
          name: "undefined table",
          tagAddr: "undefined tag",
          tagType: TagType.BOOL,
          tagNameList: Array(16).fill("")
        }))
      },
      {
        id: "2fake template",
        name: "fake template",
        sampleInterval: 498,
        hostCpuSlot: "2,0",
        tableList: Array.from({length: 10}, () => ({
          name: "fake table",
          tagAddr: "fake tag",
          tagType: TagType.BOOL,
          tagNameList: Array(16).fill("")
        }))
      }
    ] as TemplateInterface[] //  类型断言确保类型安全
    ,selectedTemplateIndex: -1
  }),

  actions: {
    //从后端数据加载
    loadFromData(data: TemplateInterface[]) {
      this.templateList = data
    },
    isTemplateNameDuplicate(newName: string): boolean {
      const normalizedNewName = newName.trim().toLowerCase();
      if (normalizedNewName === "") return false; // 空名不校验重复（或根据需求改为 true）

      return this.templateList.some(tpl =>
        (tpl.name ?? "").trim().toLowerCase() === normalizedNewName
      );
    },
    //获取所有template信息
    async getAllTemplates() {
      try {
        // 请求后端获取所有服务器信息
        let {data} = await request.get("template/getAllTemplates")
        console.log('app loaded, loading templateList from backend:', data.data.templateList)
        this.templateList = data.data.templateList
      } catch (err: any) {
        message.error('Network error, please try again');
      }
    },
    //根据templateName获取template信息
    getTemplateByName(templateName: string) {
      return this.templateList.find(tpl => tpl.name === templateName)
    }


  }
});
