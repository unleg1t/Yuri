package ddlc.yuri.api.events.impl.client;

import ddlc.yuri.api.events.Event;
import ddlc.yuri.modules.Module;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModuleEvent implements Event {
    Module module;
}