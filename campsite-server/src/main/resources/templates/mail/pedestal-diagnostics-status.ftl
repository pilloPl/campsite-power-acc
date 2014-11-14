Hello,

pedestal ${pedestalCode} (${campsite}) reports the diagnostics upload status ${status}<#if date??> at ${date}</#if>.

<#if status == "UPLOADED">The diagnostics file is on the upload server.<#else>If the upload failed, request it again from the pedestal service page.</#if>

Campsite power network
