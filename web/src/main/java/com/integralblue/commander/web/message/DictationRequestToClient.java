package com.integralblue.commander.web.message;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@Builder
public class DictationRequestToClient extends RequestToClient {

}
