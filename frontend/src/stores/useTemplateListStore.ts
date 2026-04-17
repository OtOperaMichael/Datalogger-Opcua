import {defineStore} from 'pinia'
import {NodeGroupType, DataType, type TemplateInterface} from "@/types/template.ts";
import request from "@/utils/request.ts";
import {message} from "ant-design-vue";

export const useTemplateListStore = defineStore("templateListStore", {
  state: () => ({
    templateList: [
      {
        id: "1undefined-template",
        name: "undefined-template",
        sampleInterval: 499,
        port: "4840",
        postfix: "",
        custom: {
          enable: true,
          tableList: Array.from({length: 1}, () => ({
            name: "undefined-table",
            nodeGroupType: NodeGroupType.SCALAR,
            nodeList: Array.from({length: 10}, () => ({
              name: "undefined-node",
              nodeId: "ns=2;s=undefined",
              dataType: DataType.INT
            }))
          }))
        },
        alarm: {
          enable: false,
          tableList: []
        },
        communication: {
          enable: false,
          tableList: []
        }
      },
      {
        id: "2fake-template",
        name: "fake-template",
        sampleInterval: 498,
        port: "4840",
        postfix: "",
        custom: {
          enable: true,
          tableList: Array.from({length: 1}, () => ({
            name: "fake-table",
            nodeGroupType: NodeGroupType.SCALAR,
            nodeList: Array.from({length: 10}, () => ({
              name: "fake-node",
              nodeId: "ns=2;s=fake",
              dataType: DataType.INT
            }))
          }))
        },
        alarm: {
          enable: false,
          tableList: []
        },
        communication: {
          enable: false,
          tableList: []
        }
      }
    ] as TemplateInterface[],
    selectedTemplateIndex: -1
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
