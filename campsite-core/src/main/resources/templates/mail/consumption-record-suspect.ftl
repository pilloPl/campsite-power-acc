Hello,

consumption record ${recordNo} looks implausible and waits for your review:

${reason}

Campsite: ${campsite}, pedestal ${pedestalCode}, pitch ${pitchNo}
Guest: ${guestName}
Supply: <#if plugIn??>${plugIn}<#else>unknown</#if> to <#if plugOut??>${plugOut}<#else>unknown</#if>, ${nights} night(s), ${kwh} kWh
<#if source??>Imported from partner ${source}: confirming keeps the partner's price.
</#if>
The record is not billed and not exported until it is confirmed or rejected in the review queue of the console.

Campsite power network
