/**
 * template contains multiple different modules
 * 1. custom module
 * 2. alarm module
 * 3. comm module
 * 4. xxx module
 */

//1. custom module

enum NodeType {
  SCALAR, //int, double, string, bool....
  ARRAY
}

enum DataType {
  BOOL,
  INT,
  DOUBLE,
  STRING,
}

interface CustomTagInterface {
  name: string;
  nodeId: string;
  dataType: DataType;
}

interface CustomTableInterface {
  name: string;
  nodeType: NodeType;
  nodeList: CustomTagInterface[];
}

interface CustomModuleInterface {
  enable: boolean;
  tableList: CustomTableInterface[];
}

//alarm module


//comm module

interface TemplateInterface {
  id: string;
  name: string;
  sampleInterval: number;
  port: string;
  postfix: string; //optional
  custom: CustomModuleInterface;
  alarm: CustomModuleInterface;
  communication: CustomModuleInterface;
}

export {
  NodeType,
  DataType,
  type CustomTagInterface,
  type CustomTableInterface,
  type CustomModuleInterface,
  type TemplateInterface
};




