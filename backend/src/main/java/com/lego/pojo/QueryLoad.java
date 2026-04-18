package com.lego.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lego.pojo.template.ModuleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class QueryLoad {
    private String serverName;
    
    private String moduleName;
    
    @JsonIgnore
    private ModuleType moduleType;
    
    private String tableName;
    private String startTime;
    private String endTime;
    
    public ModuleType getModuleType() {
        if (moduleType != null) {
            return moduleType;
        }
        
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        
        try {
            return ModuleType.valueOf(moduleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
