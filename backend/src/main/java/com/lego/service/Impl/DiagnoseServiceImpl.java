package com.lego.service.Impl;

import com.lego.serverTask.GlobalDataQueue;
import com.lego.service.DiagnoseService;
import com.lego.util.DBUtil;
import com.lego.util.ThreadDiagnosticUtil;

/**
 * ClassName: DiagnoseServiceImpl
 * Package: com.lego.service.Impl
 * Description:
 *
 * @Author michael.zhu
 * @Create 4/2/2026 11:21 AM
 * @Version 1.0
 */
public class DiagnoseServiceImpl implements DiagnoseService {

    @Override
    public StringBuilder getThreadStatus() {
        return ThreadDiagnosticUtil.getThreadStatus();
    }

}
