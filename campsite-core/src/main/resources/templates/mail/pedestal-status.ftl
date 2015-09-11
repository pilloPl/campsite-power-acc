Hello,

socket ${socket.name} of pedestal ${pedestalCode} (${campsite}) reports an error.

Socket code: ${socket.socketCode!"not commissioned"}
Pitch:       ${socket.pitchNo!"-"}
Status:      ${socket.status}
Errors:      <#if errorCodes?has_content>${errorCodes?join(", ")}<#else>unknown (flags ${errorFlags?c})</#if>

Please check the socket and its breaker on site. The socket is monitored again as soon as the pedestal
reports it available.

Campsite power network
