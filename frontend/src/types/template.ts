//定义template 中每个table 的类型

enum TagType {
  BOOL,
  Int,
  Dint,
  Real
}

interface TableInterface {
  name: string;
  tagAddr: string;
  tagType: TagType;
  tagList: TagInterface[];
}

interface ModuleInterface {
  enable: boolean;
  tableList: TableInterface[];
}

interface TagInterface {
  name: string;
  tagAddr: string;
  placeHolder1: string;
  placeHolder2: string;
  placeHolder3: string;
}

interface TemplateInterface {
  id: string;
  name: string;
  sampleInterval: number;
  placeHolder: string;
  hostCpuSlot: string;
  custom: ModuleInterface;
  alarm: ModuleInterface;
  communication: ModuleInterface;
}

export {TagType, type TableInterface, type ModuleInterface,  type TemplateInterface};




