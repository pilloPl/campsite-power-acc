# Klienci wyspy mechanizmu grup

Kto spoza wyspy woła klasy z listy plików wyspy (25 klas). 207 metod klienckich w 63 plikach.
Źródło: graf wywołań z wyniku wzorcowego (CodeGraph + entry-flows). Kolumna „wejście” mówi, czy metoda klienta sama jest wejściem systemu (HTTP, job, kolejka).

| Klient (Klasa.metoda) | Plik:linia | Woła w wyspie | Wejście |
|---|---|---|---|
| AdministrationApiService.auditConsoleAction | `campsite-core/src/main/java/io/pillopl/admin/AdministrationApiService.java:170,171` | Userinfo.getGroup |  |
| AdministrationApiService.reloadConfiguration | `campsite-core/src/main/java/io/pillopl/admin/AdministrationApiService.java:156` | Userinfo.getUsername |  |
| AdministrationApiService.setSystemProperty | `campsite-core/src/main/java/io/pillopl/admin/AdministrationApiService.java:107` | Userinfo.getUsername |  |
| Authorization.authenticate | `campsite-core/src/main/java/io/pillopl/card/Authorization.java:71,76,78,80,96,102,106,107` | ValidationService.validateForNow, ValidationService.getStatus, ValidationService.isHouseCardAccepted, HouseCardService.recordUse, ValidationService.getHouseCard, ValidationService.getSocket |  |
| Authorization.checkForCAMP84 | `campsite-core/src/main/java/io/pillopl/card/Authorization.java:60` | ValidationService.getStatus |  |
| Authorization.createAuthorizationResult | `campsite-core/src/main/java/io/pillopl/card/Authorization.java:120,127` | ValidationService.getStatus, ValidationService.getCard |  |
| CampingCardService.assignWelcomeCard | `campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:223` | Userinfo.getUsername |  |
| CampingCardService.checkCardAtSocket | `campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:270` | ValidationService.validateForNow |  |
| CampingCardService.checkIsManageable | `campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:291,298` | Userinfo.getUsername |  |
| CampingCardService.findWelcomePoolCards | `campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:171` | Group.getId |  |
| CampingCardService.issueCard | `campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:126` | Userinfo.getUsername |  |
| CampingCardService.welcomeCardPools | `campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:238` | GroupService.getExtensionByMarket, MarketExtension.findWelcomeCardPoolIssuerGroup |  |
| CardMapper.toCheckDTO | `campsite-core/src/main/java/io/pillopl/card/CardMapper.java:47,48,49` | ValidationService.getCard, ValidationService.getGuest, ValidationService.getStatus, ValidationService.isHouseCardAccepted |  |
| CardMapper.toDTO | `campsite-core/src/main/java/io/pillopl/card/CardMapper.java:31` | Group.getDisplayName |  |
| HouseCardApiService.changeHouseCardStatus | `campsite-core/src/main/java/io/pillopl/card/HouseCardApiService.java:93,95` | HouseCardService.activateHouseCard, Userinfo.getUsername, HouseCardService.deactivateHouseCard |  |
| HouseCardApiService.deleteHouseCard | `campsite-core/src/main/java/io/pillopl/card/HouseCardApiService.java:77` | HouseCardService.deleteHouseCard, Userinfo.getUsername |  |
| HouseCardApiService.findHouseCards | `campsite-core/src/main/java/io/pillopl/card/HouseCardApiService.java:43,44` | HouseCardService.getHouseCards, Userinfo.getUsername |  |
| HouseCardApiService.saveHouseCard | `campsite-core/src/main/java/io/pillopl/card/HouseCardApiService.java:60,63` | HouseCardService.getHouseCard, HouseCardService.saveHouseCard, Userinfo.getUsername |  |
| HouseCardMapper.toDTO | `campsite-core/src/main/java/io/pillopl/card/HouseCardMapper.java:21` | Group.getDisplayName |  |
| LocalCardListService.buildLocalCardList | `campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:127,144,151` | LocalListAttributeService.getLocalListAttribute, HouseCardService.getHouseCardsForPedestal, GroupService.findIssuerGroupIdsAcceptedAt |  |
| LocalCardListService.sendFull | `campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:76` | LocalListAttributeService.getLocalListAttribute |  |
| LocalCardListService.updateLocalListWith | `campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:207,213` | HouseCardService.getHouseCard, HouseCardService.findPedestalsForHouseCard |  |
| ConsumptionRecordApiService.checkRecordInScope | `campsite-core/src/main/java/io/pillopl/consumption/ConsumptionRecordApiService.java:132` | Userinfo.getUsername |  |
| ConsumptionRecordApiService.groupIdOf | `campsite-core/src/main/java/io/pillopl/consumption/ConsumptionRecordApiService.java:151,152` | Userinfo.getGroup, Group.getId |  |
| ConsumptionRecordApiService.isInScope | `campsite-core/src/main/java/io/pillopl/consumption/ConsumptionRecordApiService.java:141,142` | Userinfo.getGroup |  |
| ConsumptionRecordApiService.isInSubtree | `campsite-core/src/main/java/io/pillopl/consumption/ConsumptionRecordApiService.java:147` | GroupService.findManageable |  |
| ConsumptionRecordApiService.repriceConsumptionRecord | `campsite-core/src/main/java/io/pillopl/consumption/ConsumptionRecordApiService.java:107` | ConsumptionRecordWriteService.repriceRecord, Userinfo.getUsername |  |
| ConsumptionRecordApiService.requeueConsumptionRecordExport | `campsite-core/src/main/java/io/pillopl/consumption/ConsumptionRecordApiService.java:118` | ConsumptionRecordWriteService.requeueExport, Userinfo.getUsername |  |
| ConsumptionRecordApiService.reviewConsumptionRecord | `campsite-core/src/main/java/io/pillopl/consumption/ConsumptionRecordApiService.java:91,94,95` | Userinfo.getUsername, ConsumptionRecordWriteService.confirmRecord, ConsumptionRecordWriteService.rejectRecord |  |
| ErpBillingApiService.retryErpExport | `campsite-core/src/main/java/io/pillopl/erp/ErpBillingApiService.java:112` | Userinfo.getUsername |  |
| ErpBillingApiService.runErpBilling | `campsite-core/src/main/java/io/pillopl/erp/ErpBillingApiService.java:99` | Userinfo.getUsername |  |
| ErpExchangeService.exportPartnerRequests | `campsite-core/src/main/java/io/pillopl/erp/ErpExchangeService.java:82,102` | OrderService.findOrdersAwaitingErpAccount, OrderService.markOrdersExported |  |
| ErpExchangeService.importPartnerAnswers | `campsite-core/src/main/java/io/pillopl/erp/ErpExchangeService.java:124,126` | OrderService.enrichOrderWithErpNumbers, OrderService.correctErpNumbers |  |
| PedestalCommunicationLogListener.log | `campsite-core/src/main/java/io/pillopl/event/PedestalCommunicationLogListener.java:69` | CommunicationLogAttributeService.isPedestalLoggable |  |
| WelcomeCardAdoptionListener.handleEvent | `campsite-core/src/main/java/io/pillopl/event/WelcomeCardAdoptionListener.java:42` | WelcomeCardAdoptionService.checkAndAdopt |  |
| DefaultTariffService.getDefaultTariff | `campsite-core/src/main/java/io/pillopl/group/attribute/DefaultTariffService.java:35,40` | GroupService.fetchParentLineIds, GroupAttributeHelper.findNearest |  |
| DefaultTariffService.setDefaultTariff | `campsite-core/src/main/java/io/pillopl/group/attribute/DefaultTariffService.java:60,62,66,70,73,78` | Group.getId, GroupService.determineMarketByGroupId |  |
| ExportCampsiteName.computeName | `campsite-core/src/main/java/io/pillopl/group/attribute/ExportCampsiteName.java:34` | Group.getDisplayName |  |
| GroupAttribute.getGroupId | `campsite-core/src/main/java/io/pillopl/group/attribute/GroupAttribute.java:37` | Group.getId |  |
| IssuerTenantService.findFor | `campsite-core/src/main/java/io/pillopl/group/attribute/IssuerTenantService.java:35` | Group.getId |  |
| IssuerTenantService.findIssuerTenantNameFor | `campsite-core/src/main/java/io/pillopl/group/attribute/IssuerTenantService.java:55` | Group.getName |  |
| GroupManager.addAcceptanceAgreement | `campsite-core/src/main/java/io/pillopl/group/GroupManager.java:55` | Group.addAssociatedGroup |  |
| GroupManager.createChildGroup | `campsite-core/src/main/java/io/pillopl/group/GroupManager.java:28,34,35,36,37,38` | Group.getType, Group.setName, Group.setDisplayName, Group.setParent, Group.Group, Group.setType |  |
| GroupManager.createRootGroup | `campsite-core/src/main/java/io/pillopl/group/GroupManager.java:23` | Group.Group |  |
| GroupManager.removeAcceptanceAgreement | `campsite-core/src/main/java/io/pillopl/group/GroupManager.java:61` | Group.getGroups |  |
| GroupMapper.toDTO | `campsite-core/src/main/java/io/pillopl/group/GroupMapper.java:22,23` | Group.getParent, Group.getId, Group.getName, Group.getDisplayName, Group.getType |  |
| GroupTreeFactory.createNode | `campsite-core/src/main/java/io/pillopl/group/GroupTreeFactory.java:33,35,36,37,42` | Group.getId, Group.getChildren, Group.getType, Group.getName, Group.getDisplayName |  |
| GroupTreeFactory.createTree | `campsite-core/src/main/java/io/pillopl/group/GroupTreeFactory.java:29` | Group.getType |  |
| GroupType.isGroupType | `campsite-core/src/main/java/io/pillopl/group/GroupType.java:24` | Group.getType |  |
| GuestApiService.cancelOrder | `campsite-core/src/main/java/io/pillopl/guest/GuestApiService.java:204` | OrderService.cancelOrder |  |
| GuestApiService.createGuest | `campsite-core/src/main/java/io/pillopl/guest/GuestApiService.java:152` | GroupService.fetchByGroupId |  |
| GuestApiService.getGuest | `campsite-core/src/main/java/io/pillopl/guest/GuestApiService.java:101` | OrderService.findOrdersOfGuest |  |
| GuestApiService.toDTO | `campsite-core/src/main/java/io/pillopl/guest/GuestApiService.java:247` | Group.getId, Group.getDisplayName |  |
| GuestMapper.toDTO | `campsite-core/src/main/java/io/pillopl/guest/GuestMapper.java:24,25,59,60` | Group.getId, Group.getDisplayName |  |
| GuestService.changeGuestStatus | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:162` | Userinfo.getUsername |  |
| GuestService.checkIn | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:225,226,250,267,269,270` | GroupService.fetchByGroupId, Group.getType, Group.getDisplayName, Userinfo.getUsername, Group.getName |  |
| GuestService.checkIsManageable | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:432` | Userinfo.getUsername |  |
| GuestService.checkIsManageableSite | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:437,439` | GroupService.isManageable, Userinfo.getGroup, Userinfo.getUsername, Group.getName |  |
| GuestService.checkOut | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:303,309` | Userinfo.getUsername |  |
| GuestService.checkPitchIsFree | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:421` | Group.getId |  |
| GuestService.createGuest | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:132,141` | Userinfo.getUsername, Group.getType |  |
| GuestService.findGuests | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:98,100,103` | Userinfo.getGroup, Userinfo.getUsername, GroupService.findManageable |  |
| GuestService.findSeasonalContracts | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:356,357` | Userinfo.getGroup |  |
| GuestService.findStays | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:333,335,338,342,344` | Userinfo.getGroup, Userinfo.getUsername, GroupService.findManageable, GroupService.fetchByGroupId |  |
| GuestService.manageableOrNone | `campsite-core/src/main/java/io/pillopl/guest/GuestService.java:395` | GroupService.findManageable |  |
| SeasonalContractService.coversSocket | `campsite-core/src/main/java/io/pillopl/guest/SeasonalContractService.java:96` | GroupService.findManageable, Group.getId |  |
| SeasonalContractService.createContract | `campsite-core/src/main/java/io/pillopl/guest/SeasonalContractService.java:118` | Group.getId |  |
| AffiliatePedestalService.commission | `campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:175,176,190,202,219,244,247` | AffiliateGroupService.findAffiliateSiteGroup, Group.getId, Userinfo.getUsername |  |
| AffiliatePedestalService.decommission | `campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:297,300,304,307,310,311` | Userinfo.getUsername, AffiliateGroupService.findAffiliateSiteGroup, GroupService.determineMarketByGroupId, Group.getId, Userinfo.getLocale, Userinfo.getEmail |  |
| AffiliatePedestalService.findNotCommissioned | `campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:277,280` | GroupService.collectSubtreeIdsOfAll, Group.getId |  |
| AffiliatePedestalService.findSupplierGroupForPedestal | `campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:326` | GroupServiceHelper.getSupplierGroupName |  |
| AffiliatePedestalService.isCommissioned | `campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:261` | SupplierMarkerService.isInSupplierStock |  |
| AffiliatePedestalService.supplierStock | `campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:123,127,132` | Userinfo.getGroup, GroupServiceHelper.collectSubtree, Group.getId, SupplierMarkerService.isInSupplierStock |  |
| AffiliatePedestalService.switchAccessMode | `campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:360` | AffiliateGroupService.switchAccessMode |  |
| Commissioning.toString | `campsite-core/src/main/java/io/pillopl/infrastructure/Commissioning.java:78` | Userinfo.getUsername |  |
| CommissioningApiService.importSupplierPedestals | `campsite-core/src/main/java/io/pillopl/infrastructure/CommissioningApiService.java:229` | Userinfo.getUsername |  |
| CommissioningMapper.toDTO | `campsite-core/src/main/java/io/pillopl/infrastructure/CommissioningMapper.java:24,25` | Userinfo.getUsername, Userinfo.getName |  |
| CommissioningService.checkInstallerRights | `campsite-core/src/main/java/io/pillopl/infrastructure/CommissioningService.java:103,104,105` | GroupServiceHelper.collectSubtree, Userinfo.getGroup |  |
| CommissioningService.findAll | `campsite-core/src/main/java/io/pillopl/infrastructure/CommissioningService.java:66,67` | Userinfo.getGroup, GroupServiceHelper.collectSubtree |  |
| MapApiService.campsites | `campsite-core/src/main/java/io/pillopl/infrastructure/MapApiService.java:107,108,109` | Group.getId, Group.getName, Group.getDisplayName |  |
| MapApiService.getMapData | `campsite-core/src/main/java/io/pillopl/infrastructure/MapApiService.java:61,65,66,67` | SiteScopeHelper.siteGroupIdOf, GroupService.fetchByGroupId, GroupService.findManageableGroups, Group.getId |  |
| PedestalMapper.toDTO | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalMapper.java:42,43` | Group.getId, Group.getDisplayName |  |
| PedestalMapper.toSummary | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalMapper.java:61` | Group.getDisplayName |  |
| PedestalService.assignPitch | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:270` | Group.getId |  |
| PedestalService.checkIsManageable | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:160` | Userinfo.getUsername |  |
| PedestalService.deletePedestal | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:220,222` | GroupService.isManageable, Userinfo.getGroup, Userinfo.getUsername |  |
| PedestalService.findByCodeForUser | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:170,172,175` | Userinfo.getGroup, Userinfo.getUsername, Group.getId |  |
| PedestalService.findPedestalsByState | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:196,201,204,205` | Userinfo.getGroup, Group.getId |  |
| PedestalService.findPedestalsForGuest | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:183,187` | Userinfo.getGroup, Group.getId |  |
| PedestalService.isManageable | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:153` | GroupService.isManageable, Userinfo.getGroup |  |
| PedestalService.updateLocation | `campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:242,244` | GroupService.isManageable, Userinfo.getGroup, Userinfo.getUsername |  |
| SupplierPedestalImportService.findPedestalGroup | `campsite-core/src/main/java/io/pillopl/infrastructure/supplier/SupplierPedestalImportService.java:252,256,257` | GroupService.determineMarketByGroupId, Group.getId, GroupService.getExtensionByMarket, MarketExtension.findHouseCardsOnlySiteGroup, MarketExtension.findNetworkAccessSiteGroup |  |
| SupplierPedestalImportService.saveFromImport | `campsite-core/src/main/java/io/pillopl/infrastructure/supplier/SupplierPedestalImportService.java:135,136` | GroupServiceHelper.getSupplierGroupName, SupplierMarkerService.isInSupplierStock |  |
| IssuerApiService.deleteWhitelistFilter | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:240,241` | WhitelistFilterService.deleteFilter, Userinfo.getUsername |  |
| IssuerApiService.filterInScope | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:264` | GroupService.isManageable, Userinfo.getGroup |  |
| IssuerApiService.findWhitelistFilters | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:193,194` | WhitelistFilterService.findFiltersOfSubtree, Userinfo.getGroup |  |
| IssuerApiService.group | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:297` | GroupService.fetchByGroupId |  |
| IssuerApiService.issuerGroupInScope | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:252,254` | GroupService.isManageable, Userinfo.getGroup, Userinfo.getUsername |  |
| IssuerApiService.resyncCard | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:180` | Userinfo.getUsername |  |
| IssuerApiService.runIssuerExchange | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:92` | Userinfo.getUsername |  |
| IssuerApiService.saveIssuerPartner | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:151` | Userinfo.getUsername |  |
| IssuerApiService.saveWhitelistFilter | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:214,225,227` | Group.getId, WhitelistFilterService.saveFilter, Userinfo.getUsername |  |
| IssuerApiService.toDTO | `campsite-core/src/main/java/io/pillopl/issuer/IssuerApiService.java:273,305,306` | Group.getName, Group.getDisplayName, Group.getId |  |
| IssuerExchangeImportService.createConsumptionRecord | `campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeImportService.java:199,222` | Group.getId, SiteTenantService.findSiteTenantNameFor |  |
| IssuerExchangeImportService.importConsumptionRecords | `campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeImportService.java:101` | GroupService.findManageable |  |
| IssuerExchangeService.exportCards | `campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeService.java:202,209,214` | GroupService.fetchNetworkCardIssuerGroup, Group.getId, WhitelistFilterService.isSynchronizableForExport |  |
| IssuerExchangeService.exportConsumptionRecords | `campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeService.java:164` | Group.getId |  |
| IssuerExchangeService.importCards | `campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeService.java:145` | IssuerCardImportService.importCard |  |
| IssuerPartner.validate | `campsite-core/src/main/java/io/pillopl/issuer/IssuerPartner.java:128,131` | Group.getType |  |
| DashboardApiService.acknowledgeAlert | `campsite-core/src/main/java/io/pillopl/monitoring/DashboardApiService.java:131,134,135,145,146` | SiteScopeHelper.siteGroupIdOf, Userinfo.hasRight, Userinfo.getUsername |  |
| DashboardApiService.findAlerts | `campsite-core/src/main/java/io/pillopl/monitoring/DashboardApiService.java:111,116,117` | SiteScopeHelper.siteGroupIdOf, Userinfo.hasRight |  |
| DashboardApiService.getDashboard | `campsite-core/src/main/java/io/pillopl/monitoring/DashboardApiService.java:87,91,92` | SiteScopeHelper.siteGroupIdOf, Userinfo.hasRight |  |
| MonitoringApiService.resetFault | `campsite-core/src/main/java/io/pillopl/monitoring/MonitoringApiService.java:208` | Userinfo.getUsername |  |
| MonitoringApiService.scopeGroupIds | `campsite-core/src/main/java/io/pillopl/monitoring/MonitoringApiService.java:229,233,234` | SiteScopeHelper.siteGroupIdOf, GroupService.fetchByGroupId, GroupService.findManageable |  |
| MonitoringMapper.toDTO | `campsite-core/src/main/java/io/pillopl/monitoring/MonitoringMapper.java:39,40` | Group.getId, Group.getDisplayName |  |
| PedestalMailService.sendErrorMail | `campsite-core/src/main/java/io/pillopl/monitoring/PedestalMailService.java:83` | NotificationService.fetchNotification |  |
| PedestalMailService.sendStatusMail | `campsite-core/src/main/java/io/pillopl/monitoring/PedestalMailService.java:133` | NotificationService.fetchNotification |  |
| PedestalMailService.sendTimeoutMail | `campsite-core/src/main/java/io/pillopl/monitoring/PedestalMailService.java:115` | NotificationService.fetchNotification |  |
| PedestalMailService.statusNotification | `campsite-core/src/main/java/io/pillopl/monitoring/PedestalMailService.java:215` | NotificationService.fetchNotification |  |
| PedestalLinkServiceV1.socketStatus | `campsite-core/src/main/java/io/pillopl/pedestallink/v1/PedestalLinkServiceV1.java:285` | Group.getId |  |
| PedestalLinkServiceV2.socketStatus | `campsite-core/src/main/java/io/pillopl/pedestallink/v2/PedestalLinkServiceV2.java:318` | Group.getId |  |
| PedestalLinkV3Handler.processRequest | `campsite-core/src/main/java/io/pillopl/pedestallink/v3/PedestalLinkV3Handler.java:256` | Group.getId |  |
| PowerSectionService.assignSocketToSection | `campsite-core/src/main/java/io/pillopl/power/PowerSectionService.java:297` | Userinfo.getUsername |  |
| PowerSectionService.changeSectionCapacity | `campsite-core/src/main/java/io/pillopl/power/PowerSectionService.java:260,263` | Userinfo.getUsername |  |
| PowerSectionService.isInScope | `campsite-core/src/main/java/io/pillopl/power/PowerSectionService.java:379,381` | Userinfo.getGroup, GroupService.isManageable |  |
| PowerSectionService.savePowerSection | `campsite-core/src/main/java/io/pillopl/power/PowerSectionService.java:242` | Userinfo.getUsername |  |
| PowerSectionService.selectAllocationAlgorithm | `campsite-core/src/main/java/io/pillopl/power/PowerSectionService.java:279` | Userinfo.getUsername |  |
| PricingService.calculateCost | `campsite-core/src/main/java/io/pillopl/pricing/PricingService.java:57` | TariffResolver.resolve |  |
| PricingService.getPriceInformation | `campsite-core/src/main/java/io/pillopl/pricing/PricingService.java:90` | TariffResolver.resolve |  |
| TariffApiService.applyTariffToPedestals | `campsite-core/src/main/java/io/pillopl/pricing/TariffApiService.java:185` | Userinfo.getUsername |  |
| TariffApiService.applyTariffToSocket | `campsite-core/src/main/java/io/pillopl/pricing/TariffApiService.java:200` | Userinfo.getUsername |  |
| TariffApiService.findSocketTariff | `campsite-core/src/main/java/io/pillopl/pricing/TariffApiService.java:254` | TariffResolver.resolve |  |
| TariffApiService.setCampsiteDefaultTariff | `campsite-core/src/main/java/io/pillopl/pricing/TariffApiService.java:225,232,234,238,239` | GroupService.fetchByGroupId, GroupService.isManageable, Userinfo.getGroup, Userinfo.getUsername, Group.getId |  |
| TariffApplicabilityService.applyTariffToAffiliatePedestals | `campsite-core/src/main/java/io/pillopl/pricing/TariffApplicabilityService.java:70` | Userinfo.getUsername |  |
| TariffApplicabilityService.findTariffOfMarket | `campsite-core/src/main/java/io/pillopl/pricing/TariffApplicabilityService.java:198` | GroupService.determineMarketByGroupId, Group.getId |  |
| TariffApplicabilityService.setCampsiteDefaultTariff | `campsite-core/src/main/java/io/pillopl/pricing/TariffApplicabilityService.java:163` | Group.getId |  |
| TariffService.evaluateMarket | `campsite-core/src/main/java/io/pillopl/pricing/TariffService.java:238,240` | Userinfo.getUsername |  |
| TariffService.findTariffForUser | `campsite-core/src/main/java/io/pillopl/pricing/TariffService.java:64` | Userinfo.getUsername |  |
| TariffService.getAllTariffs | `campsite-core/src/main/java/io/pillopl/pricing/TariffService.java:48` | Userinfo.getUsername |  |
| TariffService.saveTariff | `campsite-core/src/main/java/io/pillopl/pricing/TariffService.java:108,113` | Userinfo.getUsername |  |
| QrPayProcessSupervisor.cancelQrPayProcess | `campsite-core/src/main/java/io/pillopl/qrpay/QrPayProcessSupervisor.java:257,259` | Userinfo.getUsername |  |
| QrPayProcessSupervisor.findChainInScope | `campsite-core/src/main/java/io/pillopl/qrpay/QrPayProcessSupervisor.java:344,345,346` | Userinfo.getGroup, GroupService.isManageable, Userinfo.getUsername |  |
| QrPayProcessSupervisor.findOwnProcess | `campsite-core/src/main/java/io/pillopl/qrpay/QrPayProcessSupervisor.java:198,199` | Userinfo.getUsername |  |
| QrPayProcessSupervisor.findQrPayProcesses | `campsite-core/src/main/java/io/pillopl/qrpay/QrPayProcessSupervisor.java:214,220` | Userinfo.getGroup, Group.getId |  |
| QrPayProcessSupervisor.runQrPayArchive | `campsite-core/src/main/java/io/pillopl/qrpay/QrPayProcessSupervisor.java:285` | Userinfo.getUsername |  |
| QrPayProcessSupervisor.startQrPay | `campsite-core/src/main/java/io/pillopl/qrpay/QrPayProcessSupervisor.java:133,137` | Userinfo.getUsername |  |
| ReportApiService.getReport | `campsite-core/src/main/java/io/pillopl/reporting/ReportApiService.java:90,91,93` | Userinfo.getGroup, GroupService.findManageable, Userinfo.getUsername |  |
| ReportApiService.groupIdOf | `campsite-core/src/main/java/io/pillopl/reporting/ReportApiService.java:176,177` | Userinfo.getGroup, Group.getId |  |
| SupplyApiService.findSupplyIncidents | `campsite-core/src/main/java/io/pillopl/supply/SupplyApiService.java:154` | SiteScopeHelper.siteGroupIdOf |  |
| SupplyApiService.findSupplySessions | `campsite-core/src/main/java/io/pillopl/supply/SupplyApiService.java:87` | SiteScopeHelper.siteGroupIdOf |  |
| SupplyApiService.reviewSupplyIncident | `campsite-core/src/main/java/io/pillopl/supply/SupplyApiService.java:178,179` | Userinfo.getUsername, Group.getId |  |
| SupplyControlUnitImpl.closeSession | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:386` | ValidationService.validateFor |  |
| SupplyControlUnitImpl.finishSession | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:407,409` | ValidationService.getSocket, ValidationService.setSocket |  |
| SupplyControlUnitImpl.hasCardWithExpiry | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:583` | ValidationService.getCard |  |
| SupplyControlUnitImpl.requestSupplyStart | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:274,276,278` | ValidationService.validateFor, ValidationService.getStatus |  |
| SupplyControlUnitImpl.saveConsumption | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:432` | ConsumptionRecordWriteService.createConsumptionRecord |  |
| SupplyControlUnitImpl.shouldConsumptionBeBilled | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:453,455` | ValidationService.isHouseCardAccepted, ValidationService.getSocket, ValidationService.isFreeSocket |  |
| SupplyControlUnitImpl.startSupply | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:110,112,116,130,140,142` | ValidationService.validateFor, ValidationService.getStatus, ValidationService.getCard, ValidationService.isHouseCardAccepted |  |
| SupplyControlUnitImpl.stopSupply | `campsite-core/src/main/java/io/pillopl/supply/SupplyControlUnitImpl.java:188,194,195,197,202` | ValidationService.validateFor, ValidationService.getStatus, ValidationService.getCard |  |
| SupplySessionManager.createNewSessionFor | `campsite-core/src/main/java/io/pillopl/supply/SupplySessionManager.java:155,156,157,170,172,174` | ValidationService.getCard, ValidationService.getHouseCard, ValidationService.getSocket, ValidationService.isHouseCardAccepted, ValidationService.isQrPayAccepted, ValidationService.getQrPayProcessId |  |
| SupplySessionMapper.toDTO | `campsite-core/src/main/java/io/pillopl/supply/SupplySessionMapper.java:39,40` | Group.getId, Group.getDisplayName |  |
| PrincipalResolver.findUser | `campsite-core/src/main/java/io/pillopl/user/PrincipalResolver.java:65` | Userinfo.isDeleted |  |
| PrincipalResolver.loadScopeAndRights | `campsite-core/src/main/java/io/pillopl/user/PrincipalResolver.java:76,77` | Userinfo.getGroups, Userinfo.getEffectiveRoles |  |
| PrincipalResolver.resolve | `campsite-core/src/main/java/io/pillopl/user/PrincipalResolver.java:32,36,51` | Userinfo.isDeleted, Userinfo.getStatus, Userinfo.hasRight |  |
| UserApiService.authenticate | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:78,86,87,91,92,93,96,97` | Userinfo.getPasswordHash, Userinfo.getUsername, Userinfo.canLogIn, Userinfo.isDeleted, Userinfo.getStatus |  |
| UserApiService.changeUserStatus | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:261` | Userinfo.getId, Userinfo.getUsername |  |
| UserApiService.createUser | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:125` | Userinfo.getId, Userinfo.getUsername |  |
| UserApiService.groupIdOf | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:103` | Userinfo.getGroup |  |
| UserApiService.replaceUserRoles | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:274` | Userinfo.getId, Userinfo.getUsername |  |
| UserApiService.setPassword | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:241,242,247` | Userinfo.getUsername, Userinfo.hasRight, Userinfo.getId |  |
| UserApiService.setupAffiliateOwner | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:167` | Userinfo.getId, Userinfo.getUsername |  |
| UserApiService.updateUser | `campsite-core/src/main/java/io/pillopl/user/UserApiService.java:138,139,144,145,146,149,150,151` | Userinfo.getUsername, Userinfo.hasRight, Userinfo.getEmail, Userinfo.getId, Userinfo.getName |  |
| UserinfoMapper.toCurrentUserDTO | `campsite-core/src/main/java/io/pillopl/user/UserinfoMapper.java:40,46,47,48` | Userinfo.getEffectiveRoles, Userinfo.getUsername, Userinfo.getName, Userinfo.getEmail, Userinfo.getStatus, Userinfo.getGroups, Userinfo.getLastLoginDate |  |
| UserinfoMapper.toDTO | `campsite-core/src/main/java/io/pillopl/user/UserinfoMapper.java:25,28,29,30,31,62` | Userinfo.getRoles, Userinfo.getId, Userinfo.getUsername, Userinfo.getName, Userinfo.getEmail, Userinfo.getStatus, Userinfo.getEmailLocale, Userinfo.getGroup, Userinfo.getGroups, Userinfo.getCreateDate, Group.getId |  |
| UserinfoService.changeEmail | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:268` | Userinfo.setEmail |  |
| UserinfoService.changeName | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:280` | Userinfo.setName |  |
| UserinfoService.changeStatus | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:518,521,523` | Userinfo.getPasswordHash, Userinfo.setStatus, Userinfo.setFailedLogins |  |
| UserinfoService.checkAffiliateOwnerSetup | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:339,340,347,351,363` | AffiliateGroupService.findAffiliateSiteGroup, Userinfo.getGroup, Group.getName, Group.getParent, Group.getId |  |
| UserinfoService.checkIsManageable | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:128` | Userinfo.getUsername |  |
| UserinfoService.createUser | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:198,204,205,206,208,210,211,213` | Userinfo.getUsername, GroupService.getExtensionByMarket, Userinfo.getGroup, GroupService.checkIsManageable, Userinfo.setGroup, Userinfo.setEmailLocale, Userinfo.getRoles, MarketExtension.findSelfServiceStaffGroup |  |
| UserinfoService.createUserinfo | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:173,174,175,176,177,178,179,180` | Userinfo.setStatus, Userinfo.setEmail, Userinfo.setName, Userinfo.setCreateDate, Userinfo.setDeleted, Userinfo.setGroups, Userinfo.setRoles, Userinfo.Userinfo |  |
| UserinfoService.determineMarket | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:93,98` | Userinfo.getGroup, GroupService.determineMarketByGroupId, Group.getId |  |
| UserinfoService.findByRoles | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:421,425` | Userinfo.getGroup, GroupService.findManageableGroups |  |
| UserinfoService.findManageable | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:569,572` | Userinfo.isDeleted, Userinfo.getUsername |  |
| UserinfoService.findManageableUsers | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:556,560` | Userinfo.getGroup, GroupService.findManageableGroups |  |
| UserinfoService.findSettlementPartnerNo | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:384,388,390,394,395,398,399` | GroupService.fetchParentLineIds, GroupService.fetchByGroupId, Userinfo.getSettlementPartnerNo, Group.getName, Group.getId |  |
| UserinfoService.isManageable | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:105,112,119` | GroupService.isManageable, Userinfo.getGroup, Userinfo.getUsername |  |
| UserinfoService.recordFailedLogin | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:473,476,477,478,480,481` | Userinfo.isDeleted, Userinfo.setFailedLogins, Userinfo.getFailedLogins, Userinfo.getStatus, Userinfo.setStatus |  |
| UserinfoService.recordSuccessfulLogin | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:462,463` | Userinfo.setLastLoginDate, Userinfo.setFailedLogins |  |
| UserinfoService.replaceRoles | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:540,542,546,547` | Userinfo.getEffectiveRoles, Userinfo.getUsername, Userinfo.getRoles |  |
| UserinfoService.saveWithDerivedLogin | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:225,226,232,240,248,250,252` | Userinfo.getEmail, Userinfo.getUsername, Userinfo.getId, Userinfo.setUsername |  |
| UserinfoService.setPassword | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:497,500,501,502,504` | Userinfo.getUsername, Userinfo.setPasswordHash, Userinfo.getStatus, Userinfo.setStatus, Userinfo.setFailedLogins |  |
| UserinfoService.setupAffiliateOwner | `campsite-core/src/main/java/io/pillopl/user/UserinfoService.java:290,292,294,295,296,303,306,311,313,316,317,319,320` | Userinfo.getUsername, GroupService.getExtensionByMarket, MarketExtension.findSelfServiceStaffGroup, MarketExtension.findHouseCardsOnlySiteGroup, AffiliateGroupService.findAffiliateSiteGroup, AffiliateGroupService.createAffiliateSiteGroup, Userinfo.getName, Userinfo.setGroups, Userinfo.setGroup, Userinfo.containsRole, Userinfo.getRoles |  |
| UserinfoSummaryService.findSelfServiceUsers | `campsite-core/src/main/java/io/pillopl/user/UserinfoSummaryService.java:39,42,45` | Userinfo.getGroup, Userinfo.getUsername, GroupService.findManageableGroups |  |
| GroupWebService.addAcceptanceAgreement | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:57` | GroupApiService.addAcceptanceAgreement | HTTP GroupWebService.addAcceptanceAgreement |
| GroupWebService.checkGroupValidity | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:84` | GroupApiService.checkGroupValidity | HTTP GroupWebService.checkGroupValidity |
| GroupWebService.createGroup | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:40` | GroupApiService.createGroup | HTTP GroupWebService.createGroup |
| GroupWebService.findAcceptanceAgreements | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:49` | GroupApiService.findAcceptanceAgreements | HTTP GroupWebService.findAcceptanceAgreements |
| GroupWebService.findManageableGroups | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:28` | GroupApiService.findManageableGroups | HTTP GroupWebService.findManageableGroups |
| GroupWebService.getGroupAttributes | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:90` | GroupApiService.getGroupAttributes | HTTP GroupWebService.getGroupAttributes |
| GroupWebService.getGroupTree | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:34` | GroupApiService.getGroupTree | HTTP GroupWebService.getGroupTree |
| GroupWebService.removeAcceptanceAgreement | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:65` | GroupApiService.removeAcceptanceAgreement | HTTP GroupWebService.removeAcceptanceAgreement |
| GroupWebService.switchAccessMode | `campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:74` | GroupApiService.switchAccessMode | HTTP GroupWebService.switchAccessMode |
| GroupValidityControlJob.executeInternal | `campsite-server/src/main/java/io/pillopl/jobs/GroupValidityControlJob.java:28` | GroupService.collectValidityErrors | job (Quartz) GroupValidityControlJob.executeInternal |
| JobControlService.pauseJob | `campsite-server/src/main/java/io/pillopl/jobs/JobControlService.java:104` | Userinfo.getUsername |  |
| JobControlService.resumeJob | `campsite-server/src/main/java/io/pillopl/jobs/JobControlService.java:119` | Userinfo.getUsername |  |
| JobControlService.runJobNow | `campsite-server/src/main/java/io/pillopl/jobs/JobControlService.java:89` | Userinfo.getUsername |  |
