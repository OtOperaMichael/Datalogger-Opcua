interface SystemConfigInterface{
  adminPassword: string;
  maxServerCount: number;
  maxTableCount: number;
  maxTableFieldCount: number;
  dataCollectionThreads: number;
  dataSaveThreads: number;
  dataSaveInterval: number;
  dataSaveBatchSize: number;
  logLevel: number;
}


export { type SystemConfigInterface };
