/**
 * template contains multiple different modules
 * 1. custom module
 * 2. alarm module
 * 3. comm module
 * 4. xxx module
 */

//1. custom module

enum NodeGroupType {
  SCALAR, //int, double, string, bool....
  ARRAY
}

enum DataType {
  BOOL,
  INT,
  DOUBLE,
  STRING,
}

interface CustomNodeInterface {
  name: string;
  nodeId: string;
  dataType: DataType;
}

interface CustomTableInterface {
  name: string;
  sampleInterval: number;
  nodeGroupType: NodeGroupType;
  nodeList: CustomNodeInterface[];
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
  port: string;
  postfix: string; //optional
  custom: CustomModuleInterface;
  alarm: CustomModuleInterface;
  communication: CustomModuleInterface;
}

export {
  NodeGroupType,
  DataType,
  type CustomNodeInterface,
  type CustomTableInterface,
  type CustomModuleInterface,
  type TemplateInterface
};




