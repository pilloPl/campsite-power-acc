No service recipient (notification ${notificationType}) is configured above pedestal ${pedestalCode} (${campsite}).
Please add a notification to its campsite or section.

<#if socket??>Socket ${socket.name}, status ${socket.status}, errors: <#if errorCodes?has_content>${errorCodes?join(", ")}<#else>flags ${errorFlags?c}</#if>
</#if><#if status??>Reported status: ${status}<#if date??> at ${date}</#if>
</#if>