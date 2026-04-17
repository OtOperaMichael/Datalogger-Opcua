/**
 * template contains multiple different modules
 * 1. custom module
 * 2. alarm module
 * 3. comm module
 * 4. xxx module
 */

// Module Type Enum
enum ModuleType {
  CUSTOM,
  ALARM,
  COMMUNICATION,
}

// Common types
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

// Custom Module Types
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

// Alarm Module Types
enum TriggerType {
  RISING,   // 上升沿触发
  FALLING   // 下降沿触发
}

interface AlarmNodeInterface {
  name: string;
  nodeId: string;
  triggerType: TriggerType;
  description: string;
}

interface AlarmTableInterface {
  name: string;
  sampleInterval: number;
  nodeGroupType: NodeGroupType;
  nodeList: AlarmNodeInterface[];
}

interface AlarmModuleInterface {
  enable: boolean;
  tableList: AlarmTableInterface[];
}

// Communication Module Types
interface CommNodeInterface {
  name: string;
  nodeId: string;
}

interface CommTableInterface {
  name: string;
  sampleInterval: number;
  nodeGroupType: NodeGroupType;
  nodeList: CommNodeInterface[];
}

interface CommModuleInterface {
  enable: boolean;
  tableList: CommTableInterface[];
}

// Template Interface
interface TemplateInterface {
  id: string;
  name: string;
  port: string;
  postfix: string; //optional
  custom: CustomModuleInterface;
  alarm: AlarmModuleInterface;
  communication: CommModuleInterface;
}

export {
  ModuleType,
  NodeGroupType,
  DataType,
  TriggerType,
  type CustomNodeInterface,
  type CustomTableInterface,
  type CustomModuleInterface,
  type AlarmNodeInterface,
  type AlarmTableInterface,
  type AlarmModuleInterface,
  type CommNodeInterface,
  type CommTableInterface,
  type CommModuleInterface,
  type TemplateInterface
};




