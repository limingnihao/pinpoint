package com.navercorp.pinpoint.web.plugin;

import com.navercorp.pinpoint.common.server.bo.SpanBo;

import java.util.List;

public interface PluginSpanAlignerNext {

    List<SpanBo> process(List<SpanBo> inputList);

}
