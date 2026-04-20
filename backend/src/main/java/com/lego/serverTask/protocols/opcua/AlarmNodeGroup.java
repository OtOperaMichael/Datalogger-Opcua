package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.custom.Table;
import lombok.Getter;

public class AlarmNodeGroup extends OpcUaNodeGroup {

    // full table name: module_type_table_name
    @Getter
    private String fullTableName;


    public AlarmNodeGroup(String serverName, ModuleType moduleType, Table table) {

        super(serverName, moduleType, table);
        this.fullTableName = moduleType.toString().toLowerCase() + "_" + "history";

    }

}
