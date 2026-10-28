# Archetypy — skan: „Czy ta karta dostanie prąd na tym kempingu?”

## Wejście skanu

Agenci skanu dostali dwa pliki: PROBLEM (poniżej, dosłownie) oraz kontekst systemu `analiza/business-context.md`. Kodu nie widzieli. Skan w wersji skróconej (przebieg testowy): 4 archetypy — party, pricing, accounting, product.

# PROBLEM

Paczka pytań: Czy ta karta dostanie prąd na tym kempingu?
Co się zmienia: w gnieździe płynie prąd albo nie. Kto zauważy: gość przy słupku, personel po odmowie.

Pytanie 1: Czy wydawca karty gościa ma z siecią umowę, że jego karty działają na kempingu, na którym stoi ten słupek?
Podsumowanie: Naprawdę pyta tylko słupek, gdy gość przykłada kartę: przy autoryzacji, starcie dostawy, zdalnym starcie z konsoli, próbie na sucho operatora i przy zatrzymaniu cudzą kartą. Ekran umów wydawcy nie pyta o konkretny słupek, tylko pokazuje operatorowi, na których kempingach działają karty wydawcy. Trzy wejścia po stronie kempingu i kodu słupka istnieją, ale w działaniu sieci nikt ich nie używa.
- Operator sieci otwiera umowy wydawcy kart, dodaje albo usuwa umowę z kempingiem i chce zobaczyć, na których kempingach karty tego wydawcy teraz działają i czy te umowy są jego własne, czy przejęte od wydawcy wyżej. Nie pyta o jeden słupek, pyta o cały zasięg umów wydawcy.
- Na których kempingach działają karty tego wydawcy, wliczając kempingi pod kempingami z umowy — to wspólna odpowiedź, z której korzysta słupek przy karcie gościa i ekran umów operatora; samo nie ma swojej sytuacji.
- Gość przykłada kartę do gniazda słupka (albo operator z konsoli uruchamia prąd lub sprawdza kartę na gnieździe): czy wydawca tej karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie rusza.

Pytanie 2: Karty których wydawców przyjmuje kemping tego słupka?
Podsumowanie: Jedno pytanie zadawane, gdy słupek dostaje pełną lokalną listę kart (z konsoli albo przy rozsyłce do wszystkich słupków) i gdy personel ją tylko podgląda. Odpowiedź to wydawcy, których umowa obejmuje kemping słupka lub coś wyżej w drzewie.
- Słupek dostaje pełną listę kart, które rozpozna bez sieci (przy rozsyłce do słupków albo na żądanie z konsoli), albo personel ją podgląda: karty których wydawców przyjmuje kemping tego słupka? Karty innych wydawców nie trafiają na listę, więc bez łączności słupek ich nie wpuści.

Pytanie 3: Na których kempingach mogą ładować karty tego wydawcy (tego gościa) i czy to umowa wydawcy, czy odziedziczona po organizacji nadrzędnej?
Podsumowanie: Jedyny żywy przypadek to ekran umów w konsoli: operator sprawdza, na których kempingach działają karty wydawcy i czy to jego własna umowa, czy przejęta po organizacji nadrzędnej (także zaraz po dodaniu lub zdjęciu umowy). Wersja „na których słupkach w moim zakresie gość może ładować” istnieje w kodzie, ale nikt jej nie woła.
- Operator w konsoli otwiera umowy wydawcy: na których kempingach goście z kartami tego wydawcy mogą ładować i czy to umowa samego wydawcy, czy przejęta po organizacji nadrzędnej? Wydawca spoza jego zakresu albo coś, co nie jest wydawcą: odmowa.
- Operator dodał albo zdjął umowę wydawcy z kempingiem (albo tylko ją podgląda): na których kempingach karty tego wydawcy działają teraz i czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?

Pytanie 4: Czy to karta personelu, której wolno ładować na tym kempingu bez ograniczeń (i dla jakiego obszaru ją wydano)?
Podsumowanie: Pytanie zadaje słupek przy każdym przyłożeniu karty (start ładowania, zatrzymanie, zdalny start, sprawdzenie przez personel): karta personelu kempingu lub jego regionu ładuje od razu, bez reszty sprawdzeń. Odpowiedź z góry dostaje też słupek w swojej lokalnej liście kart. Kto ma tę odpowiedź, ustalają dwa miejsca: operator kempingu, który wydaje kartę personelu, i przejęcie karty powitalnej gościa, po którym ta karta staje się kartą personelu.
- Gość albo pracownik przykłada kartę do gniazda słupka i chce ładować: czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, prąd rusza od razu, nawet przy zajętym gnieździe i wyczerpanej mocy sekcji. Jeśli nie, karta przechodzi zwykłe sprawdzenia gościa.
- Operator kempingu zakłada lub zmienia kartę personelu w konsoli. Nie pyta, czy karta jest kartą personelu, tylko ustala odpowiedź: od teraz ta karta ładuje bez ograniczeń na całym jego kempingu (lub przestaje, gdy ją wyłączy). Słupki kempingu mają się o tym dowiedzieć.
- Słupek dostaje z góry listę kart, które może przyjąć bez łączności z siecią: które karty personelu kempingu i jego regionu ma znać? Karta personelu, której nie ma na liście, nie zadziała przy braku łączności. Gdy kart personelu jest więcej niż miejsca, karty gości wypadają z listy.
- Gość z kartą powitalną sieci został odrzucony na słupku kempingu partnerskiego, który wpuszcza tylko karty personelu. Czy kemping zgadza się przejąć tę kartę? Jeśli tak, karta gościa staje się kartą personelu kempingu i odtąd ładuje tam bez ograniczeń. Tu nikt nie pyta, czy to karta personelu, tylko się ją nią robi.
- Przy każdym sprawdzeniu karty na słupku (start ładowania, zatrzymanie inną kartą, zdalny start z konsoli, próbne sprawdzenie przez personel): czy to karta personelu tego kempingu lub jego regionu? Jeśli tak, jest przyjęta od razu, bez sprawdzania, czy kemping przyjmuje karty jej wydawcy i czy karta jest ważna.

Pytanie 5: Czy kemping chce przejąć kartę powitalną, którą gość zostawił, jako własną kartę obsługi — i czy to w ogóle karta powitalna z puli tego kraju?
Podsumowanie: Pytanie zadaje naprawdę tylko jedno miejsce: zaraz po tym, jak słupek odmówił karcie, bo kemping jej nie przyjmuje, system w tle sprawdza, czy to karta powitalna z puli kraju słupka i czy kemping afiliowany (tylko karty domowe, z włączonym przejmowaniem) ją przejmie. Ekran grupy pokazuje operatorowi tylko ustawienie „przejmowanie dozwolone”, a recepcja korzysta z tej samej puli w innym celu: sprawdza, czy kartę z puli dowolnego kraju można wydać gościowi.
- Gość przykłada kartę do słupka i dostaje odmowę, bo kemping tej karty nie przyjmuje: czy warto w tle sprawdzić, czy kemping przejmie ją jako własną kartę obsługi? Inne odmowy (np. karta nieważna, gniazdo zajęte) niczego nie uruchamiają.
- Operator ogląda ustawienia grupy (kempingu, regionu), którą zarządza: czy przejmowanie porzuconych kart powitalnych jest tu włączone (wprost albo odziedziczone z wyższego poziomu)? Widzi tylko ustawienie, nie to, czy przejęcie by się udało (kraj karty, tryb tylko karty domowe, afiliacja).
- Słupek właśnie odmówił karcie, bo kemping jej nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu, a kemping afiliowany przyjmuje tylko własne karty i pozwala przejmować porzucone karty powitalne? Jeśli tak, karta staje się kartą obsługi właściciela kempingu i znika z puli; jeśli nie, nic się nie dzieje.

Pytanie 6: Czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi?
Podsumowanie: Tylko jedno miejsce naprawdę pyta, czy niezależny kemping przyjmuje wyłącznie własne karty: odrzucenie karty powitalnej gościa na słupku, które może zamienić ją w kartę obsługi kempingu. Reszta to ekran atrybutów kempingu (pokazanie odpowiedzi) i dwa miejsca, które odpowiedź ustawiają (operator w konsoli i instalator przy uruchamianiu słupków) — przestawienie trybu przenosi kemping w drzewie i zmienia widoczność słupków w aplikacji.
- Instalator kończy uruchamianie słupków na niezależnym kempingu i ustawia, czy kemping ma być otwarty na karty sieci, czy przyjmować tylko własne karty obsługi. Nie pyta o odpowiedź, tylko ją wyznacza; pyta jedynie, czy kemping ma już jakikolwiek tryb. Jeśli nie, uruchomienie kończy się sukcesem, a tryb po cichu się nie zmienia.
- Operator w konsoli przestawia niezależny kemping między „otwarty na karty sieci” a „tylko własne karty obsługi”. Nie pyta o odpowiedź, tylko ją zmienia; pyta jedynie, czy to w ogóle kemping niezależny z ustalonym trybem. Jeśli nie, dostaje odmowę.
- Operator (albo wsparcie) ogląda atrybuty kempingu w konsoli: czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi? Jeśli tryb nie daje się ustalić, pole jest puste i nie wiadomo dlaczego.
- Gość przykłada do słupka kartę powitalną z puli rynku, a kemping ją odrzucił: czy ten niezależny kemping przyjmuje tylko własne karty obsługi (i zgodził się przygarniać karty powitalne)? Jeśli tak, karta staje się kartą obsługi tego kempingu i znika z puli powitalnej; jeśli kemping jest otwarty na karty sieci, karta zostaje odrzucona i nic się nie dzieje.

Pytanie 7: Czy ten słupek ma pamiętać listę kart, które może przyjąć bez pytania centrali, i jakie karty ma na niej trzymać (karty obsługi, karty gości)?
Podsumowanie: Pytanie pada w trzech sytuacjach: operator podgląda w konsoli, co słupek dostanie; słupek dostaje pełną listę (ręcznie z konsoli albo przy rozsyłce do wszystkich); zmiana karty obsługi idzie na słupki, które ją trzymają. Za każdym razem decyduje najbliższe ustawienie wyżej w drzewie sieci, a przy kartach obsługi drzewo karty, nie słupka.
- Operator w konsoli podgląda słupek: jaką listę kart słupek dostałby teraz, żeby wpuszczać bez pytania centrali? Gdy w sieci nad słupkiem nie włączono listy, widzi pustą.
- Słupek dostaje pełną listę kart (operator wysyła ją z konsoli albo lista idzie na wszystkie słupki naraz): które karty obsługi i które karty gości akceptowanych wydawców słupek ma pamiętać, skoro mieści tylko tyle, ile pozwala jego typ? Karty gości wypadają pierwsze.
- Zmieniła się karta osoby z obsługi: które słupki trzymają ją na swojej liście i muszą dostać zmianę? Decyduje ustawienie listy nad kartą (jej miejscem w sieci), a nie nad słupkiem; gdy nad kartą nie włączono kart obsługi na listach, żaden słupek jej nie dostaje.
- Czy w tym miejscu sieci słupki mają pamiętać listę kart i co ma na niej być? Rozstrzyga najbliższe ustawienie wyżej w drzewie: przy podglądzie i wysyłce listy na słupek, przy zmianie karty obsługi i na ekranie ustawień w konsoli.

Pytanie 8: Czy karty tego wydawcy wolno wymieniać z tym partnerem — wysyłać mu je i przyjmować od niego?
Podsumowanie: Dwa przypadki w dwóch kierunkach: przy wysyłce listy kart sieci do partnera decyzja zapada osobno dla każdej karty (karta pominięta, ale i tak uznana za wysłaną), a przy przyjmowaniu kart od partnera — dla całego wydawcy partnera (odrzucona każda linia). W obu rozstrzyga najbliższe ustawienie w górę drzewa wydawców.
- Przy wymianie z partnerami (cyklicznie albo na żądanie operatora) sieć wysyła partnerowi listę swoich kart: czy kartę tego gościa wolno pokazać temu partnerowi? Jeśli nie, partner jej nie dostaje i jego słupki jej nie znają.
- Partner przysyła zmiany w swoich kartach: czy od tego wydawcy w ogóle przyjmujemy karty? Jeśli nie, żadna jego karta nie zostaje dodana, zmieniona ani usunięta, więc jego goście nie ładują na nowych kartach.
- Wspólna reguła obu kierunków wymiany: dla wydawcy karty sprawdza się ustawienie na nim samym, a jak go nie ma — na najbliższym wyżej w drzewie wydawców; ono mówi, czy z tym partnerem wymieniamy. Sama nie zna przypadku — ten opisują wysyłka i przyjmowanie kart.

Pytanie 9: Czy to karta sieci, czy karta wydana przez partnera — i czy przez tego partnera, który ją teraz przysyła?
Podsumowanie: Jedno pytanie zadawane wyłącznie przy nocnej (albo ręcznie uruchomionej przez administratora) wymianie list kart z partnerem-wydawcą: zanim karta z listy partnera zostanie przyjęta, zmieniona albo wyłączona, system sprawdza, czy to nie karta sieci i czy należy do tego wydawcy, który ją przysyła. Wszystkie pozostałe wiersze to kroki tej samej odpowiedzi, bez osobnych pytających.
- Co noc, albo na żądanie administratora, partner-wydawca przysyła zmiany swojej listy kart: dla każdej karty — czy to nie karta sieci i czy należy do tego partnera, a nie do innego wydawcy? Jeśli nie, ta jedna karta zostaje odrzucona, a pozostałe karty z listy przechodzą dalej.
- Partner przysyła dodanie, zmianę albo wyłączenie karty, którą już znamy jako kartę wydawcy: czy to jego karta, czy innego wydawcy? Jeśli innego, zmiana jest odrzucona i karta zostaje u dotychczasowego wydawcy.
- Partner przysyła kartę, którą sieć już ma: czy to karta wydana przez sieć? Jeśli tak, partner nigdy jej nie przejmie, niezależnie od tego, co chce z nią zrobić.
- Partner przysyła kartę: czy znamy ją już jako kartę jakiegoś wydawcy, wtedy dalej sprawdzamy, czy jego,, czy jest dla nas nowa? Przy dodaniu nowa karta zostaje założona dla partnera; przy zmianie albo wyłączeniu nieznana karta jest odrzucona jako nieznaleziona.
- Które karty są kartami wydanymi przez sieć? Potrzebne tylko po to, by przy wymianie z partnerem odróżnić karty sieci od kart wydawców.

Pytanie 10: Jak jest zorganizowana sieć, za którą odpowiadam: które kempingi należą do których regionów i rynków (a wydawcy i zespoły — do kogo)?
Podsumowanie: Jedno pytanie, jeden przypadek: personel w konsoli (zakładka organizacji) ogląda drzewo sieci w swoim zakresie — kempingi, wydawców albo zespoły. Wszyscy wołający to kolejne piętra tej samej ścieżki pokazującej drzewo; nikt nie podejmuje na nim decyzji.
- Osoba z personelu otwiera w konsoli zakładkę organizacji: jak wygląda sieć w jej zakresie — kempingi w regionach i rynkach (albo wydawcy, albo zespoły)? Widzi drzewo od swojego miejsca w dół.

## Raport

**Wynik:** jedno wystąpienie — Party. Pricing, Accounting i Product nie pasują.

| wystąpienie (język biznesu) | archetyp | kluczowy dowód (cytat) | zastrzeżenia |
|---|---|---|---|
| Wydawcy kart w powiązaniach organizacyjnych: wydawca, jego organizacja nadrzędna i podgrupy, partner, który jest też kempingiem przyjmującym karty sieci, oraz prawo ładowania osób z obsługi zależne od tego, do którego kempingu lub regionu należą | Party | „na których kempingach goście z kartami tego wydawcy mogą ładować i czy to umowa samego wydawcy, czy przejęta po organizacji nadrzędnej?” | „organizacja nadrzędna” może być tylko folderem w konsoli; wielorolowość partnera — jedno zdanie; prawo ładowania obsługi dziś wisi na karcie, nie na osobie |

- podmioty z tożsamością ← wydawca kart (partner) — „Mają z siecią umowy akceptacji: ich karty działają na wskazanych kempingach sieci”
- relacje między podmiotami, od których zależy logika ← wydawca – organizacja nadrzędna — „czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?”
- wielorolowość (trop) ← partner wydaje karty i przyjmuje karty sieci — „Działa to też w drugą stronę: goście sieci ładują u partnera”
- zdolności ← prawo ładowania obsługi zależne od kempingu, regionu, słupków, miesiąca — „Ekipy remontowe jeżdżą po kilku kempingach…”

**Nie pasują**
- Pricing: wszystkie 10 pytań dotyczy dostępu do prądu, nie ceny; taryfa z kontekstu (pora dnia, progi kWh) leży poza problemem.
- Accounting: pytania o stan i zgodę („czy”, „które”), nigdzie „ile”; storno linii ERP, pakiety mocy i rekordy zużycia są poza problemem i bez salda zmienianego operacjami.
- Product: umowa akceptacji to zgoda na zasięg dostępu, nie definicja oferty; rodzaj karty to atrybut uprawnień; pakiety mocy tylko z nazwy.

## Opowieść

W problemie „czy ta karta dostanie prąd” występuje jeden archetyp: Party, w powiązaniach wydawców kart.

Wydawca kart (podmiot, Party) to partner, o którym sieć pamięta umowy, karty i ustawienia wymiany. Wydawcy mają organizacje nadrzędne i podgrupy (relacje między podmiotami), a od tych relacji zależy, czy karta gościa dostanie prąd: „czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?”. Partner bywa też drugą stroną — „goście sieci ładują u partnera” (ten sam podmiot w dwóch rolach). Prawo ładowania obsługi zależy od tego, do którego kempingu lub regionu ktoś należy (zdolność wynikająca z relacji), choć dziś jest przypięte do karty.

Czego nie ma: ceny (taryfa jest w systemie, ale nie w tym problemie), sald i ruchów wartości ani katalogu oferty.

Wystąpienia są niezależne; reguły łączenia nie dopuszczają tu żadnego połączenia.

## Wystąpienia od agentów

### party

```
ARCHETYP: party
WERDYKT ZBIORCZY: PASUJE

WYSTĄPIENIA:
1. Wydawcy kart w powiązaniach organizacyjnych: wydawca, jego organizacja nadrzędna i podgrupy, partner, który jest też kempingiem przyjmującym karty sieci, oraz prawo ładowania osób z obsługi zależne od tego, do którego kempingu lub regionu należą
   werdykt: PASUJE
   pytanie główne: TAK — „na których kempingach goście z kartami tego wydawcy mogą ładować i czy to umowa samego wydawcy, czy przejęta po organizacji nadrzędnej?" (problem.md:18)
   elementy:
     - podmioty z tożsamością ← wydawca kart (partner) jako podmiot, o którym system pamięta umowy, karty i ustawienia wymiany (nie tylko login) — „Mają z siecią umowy akceptacji: ich karty działają na wskazanych kempingach sieci" (business-context.md:75); „dla wydawcy karty sprawdza się ustawienie na nim samym, a jak go nie ma — na najbliższym wyżej w drzewie wydawców; ono mówi, czy z tym partnerem wymieniamy" (problem.md:53)
     - relacje między podmiotami, od których zależy logika ← wydawca należy do organizacji nadrzędnej i korzysta z jej umów; od tego zależy, czy słupek da prąd — „czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?" (problem.md:19); „Sieć ma też wydawców międzynarodowych i wydawców z podgrupami" (business-context.md:78); „Odpowiedź to wydawcy, których umowa obejmuje kemping słupka lub coś wyżej w drzewie." (problem.md:13)
     - wielorolowość (trop) ← partner jest wydawcą kart i zarazem miejscem, gdzie ładują goście sieci (strona przyjmująca cudze karty) — „Działa to też w drugą stronę: goście sieci ładują u partnera" (business-context.md:76)
     - zdolności podmiotów (kto może ładować, gdzie i kiedy) ← prawo ładowania obsługi zależne od kempingu/regionu/słupków/miesiąca — „Ekipy remontowe jeżdżą po kilku kempingach. Potrzebują jednej karty, która działa tam, gdzie są w danym miesiącu przypisane" (business-context.md:86); „Pracownik sezonowy przy marinie ma ładować tylko przy słupkach od 3 do 7, nigdzie indziej." (business-context.md:84)
   elementy bez dowodu: wymienność osoby i organizacji w tej samej roli (element 4)
   anty-sygnały: „Każdy podmiot ma jedną stałą rolę, a relacje nie wpływają na logikę" — nie zachodzi: relacja wydawca–organizacja nadrzędna decyduje o prądzie (problem.md:10, 13, 19) i o wymianie kart z partnerem (problem.md:53), a partner bywa też stroną przyjmującą (business-context.md:76). „Mały system z banalną listą klientów" — wejście mówi odwrotnie: „Sieć ciągle rośnie" (business-context.md:26), „wydawcy międzynarodowi i wydawcy z podgrupami" (business-context.md:78).
   zastrzeżenia: „drzewo wydawców" i „organizacja nadrzędna" mogą być językiem konsoli — drzewo grup to struktura systemu („jak foldery", business-context.md:151), więc nie wiadomo, czy podgrupa wydawcy to realna relacja biznesowa (oddział, spółka zależna), czy tylko dziedziczenie ustawień w folderach; logika na pewno z niej korzysta. Wielorolowość partnera potwierdza tylko jedno zdanie. Prawo ładowania obsługi dziś wiąże się z kartą, nie z osobą; przypadki osób w czasie i miejscu (CAMP-210, CRD-201) to życzenia biznesu, nie obecna mechanika.
```

Pytań z FAZY 2 nie było (żadne wystąpienie NIEROZSTRZYGNIĘTE).

## Pogłębienie: party

Prompt: `/archetype-storyteller Pogłębienie: party. Skan: analiza/archetypy.md. Miejsca: ValidationService.isCardAcceptedAt, LocalCardListService.buildLocalCardList, WelcomeCardAdoptionService.checkPedestal, IssuerExchangeService.exportCards, IssuerCardImportService.isNetworkCard` + bolączki z adresami w kodzie (poniżej). Tryb: wskazane miejsca. Jedna runda pytań.

### Wynik

Party zmapowane na wydawców kart w powiązaniach organizacyjnych; 5 miejsc w tabeli „Co i jak”.

| wystąpienie | archetyp | poziom złożoności | status mapowania | liczba założeń |
|---|---|---|---|---|
| Wydawcy kart w powiązaniach organizacyjnych (wydawca, organizacja nadrzędna i podgrupy, partner, który jest też kempingiem przyjmującym karty sieci, prawo ładowania obsługi zależne od kempingu lub regionu) | Party | 3 | KOMPLETNE | 7 |

### Opowieść

Bez zmian względem skanu (sekcja „Opowieść”): jedno wystąpienie, bez połączeń.

### Mapowanie

### Party Archetype Model: Kto ma prawo do prądu w sieci kempingów

#### Complexity Level
Poziom 3. Ten sam partner wydaje karty i przyjmuje na swoich kempingach karty sieci: „Działa to też w drugą stronę: goście sieci ładują u partnera” (business-context.md:76). Wydawcy mają strukturę: „Sieć ma też wydawców międzynarodowych i wydawców z podgrupami” (business-context.md:78). Logika zależy od relacji: „czy wydawca ma już własną umowę, czy nadal korzysta z umowy organizacji nadrzędnej?” (archetypy.md:25). Sieć ma strukturę wewnętrzną (kempingi, regiony, rynki) i właścicieli afiliowanych.
**W zakresie:** typy podmiotów, jeden PartyId, identyfikator karty, role globalne, relacje z ważnością i regułami, uprawnienia wynikające z relacji (prawo do prądu, prawo ładowania obsługi, wymiana kart z partnerem).
**Poza zakresem:** zdolności i wymagania ról (nikt nie pyta „kto umie”, tylko „kto ma prawo”), adresy, rozliczenia.

#### Concept Mapping

| Domain Concept | Party Archetype Element | Notes |
|---|---|---|
| Sieć (operator kempingów) | Company + role ISSUER, NETWORK_OPERATOR | Sieć też wydaje karty; dziś „karta sieci” to poddrzewo grupy „NETWORK CARD” (bolaczki:236) |
| Wydawca kart (partner) | Company + rola globalna ISSUER | „partner wydający karty” (business-context.md:147) |
| Partner, u którego ładują goście sieci | ta sama Company + rola ACCEPTOR | jeden podmiot w dwóch rolach (business-context.md:76) |
| Organizacja nadrzędna wydawcy | Company + relacja ISSUER_MEMBERSHIP (to) | jej umowy sumują się z umowami podgrup (odpowiedź 1) |
| Podgrupa wydawcy | OrganizationUnit albo Company + ISSUER_MEMBERSHIP (from) | |
| Kemping | OrganizationUnit (sieci albo właściciela afiliowanego) | business-context.md:137 |
| Region | OrganizationUnit sieci + relacja REGION_MEMBERSHIP | raportowanie (business-context.md:149); daje prawo do prądu tylko przez umowę „na region z przyszłymi kempingami” (odpowiedź 2) |
| Rynek | OrganizationUnit sieci + identyfikator MARKET_CODE | business-context.md:150 |
| Właściciel afiliowany | Company albo Person (nieznane) + rola AFFILIATE_OWNER + SITE_OWNERSHIP | dziś to „reszta nazwy po prefiksie” (bolaczki:143) |
| „Otwarty na karty sieci” / „tylko własne karty obsługi” | jest albo nie ma ACCEPTANCE_AGREEMENT: Sieć → kemping afiliowany | (Z) zamiast prefiksu nazwy rodzica (bolaczki:138) |
| Umowa akceptacji | relacja ACCEPTANCE_AGREEMENT (ISSUER → kemping albo region) z atrybutem zasięgu | business-context.md:148; odpowiedź 2 |
| Umowa na kempingi / na region bez przyszłych | zasięg STATIC: lista kempingów ustalona przy zawarciu | nie podąża za przeniesieniem |
| Umowa „na region z przyszłymi kempingami” | zasięg FOLLOWS_REGION: liczona po bieżącym REGION_MEMBERSHIP | podąża za przeniesieniem |
| „Umowa przejęta od organizacji nadrzędnej” | ścieżka ISSUER_MEMBERSHIP\* → ACCEPTANCE_AGREEMENT, suma po ścieżce | dziś listy wyżej są zastępowane (bolaczki:25); w modelu się sumują |
| Gość, posiadacz karty | Person + rola CARDHOLDER w CARD_ISSUANCE | |
| Karta (UID RFID) | RegisteredIdentifier CARD_UID posiadacza, z ważnością | stan karty (blokada) poza jądrem |
| Wydanie karty przez wydawcę | relacja CARD_ISSUANCE (ISSUER → CARDHOLDER) | bolaczki:215 |
| Karta sieci | CARD_ISSUANCE, w której wydawcą jest Sieć | bolaczki:220 |
| Osoba z obsługi, dozorca, ekipa, sezonowy | Person + STAFF_ASSIGNMENT do konkretnego kempingu | odpowiedź 3; business-context.md:84–86 |
| Karta domowa / karta obsługi | CARD_UID osoby z obsługi + uprawnienie CHARGE_AS_STAFF | dziś prawo wisi na karcie i poddrzewie (bolaczki:96) |
| Karta obsługi „na region” | skrót przy wydaniu: rozwinięcie do listy STAFF_ASSIGNMENT na kempingi regionu w tej chwili | odpowiedź 3 |
| Przygarnięcie karty powitalnej | koniec CARD_ISSUANCE z pulą, początek STAFF_ASSIGNMENT u właściciela kempingu | archetypy.md:39 |
| Pula kart powitalnych rynku | karty wydane przez jednostkę „Rynek X” sieci, bez posiadacza | (Z) |
| Partner wymiany (eksport/import) | EXCHANGE_PARTNERSHIP (Sieć → Partner) z kierunkami | business-context.md:74 |
| Filtr eksportu/importu | direct deny dla pary partner–wydawca | dziś najbliższy filtr w linii rodziców (bolaczki:194) |
| Operator konsoli i jego zakres | Person + RESPONSIBLE_FOR (do jednostki) | konto poza jądrem |
| Dodać / zdjąć umowę | rozpoczęcie / zakończenie relacji (zdarzenia) | archetypy.md:25 |
| Przenieść kemping do regionu | zmiana REGION_MEMBERSHIP | wpływa na prawo do prądu wyłącznie przez umowy FOLLOWS_REGION |

#### Unmapped Concepts
- Słupek, gniazdo, stanowisko: to nie podmioty, słupek jest tylko rzutem na kemping.
- Budżet mocy sekcji, zajętość gniazda, sesja poboru.
- Ważność i blokada karty jako cykl życia.
- Lokalna lista kart: pojemność, kolejność, ucinanie, rozsyłka, wersje; ustawienie LocalList.
- Pula kart powitalnych jako zapas kart do wydania na recepcji.
- Mechanika wymiany z partnerami: pliki, harmonogram, oznaczanie eksportu.
- Widoczność w aplikacji, zdalne sterowanie, „rozliczany przez kemping”.
- Płatność QR, taryfy, rozliczenia z wydawcami.

#### Party Types

| Party Type | Represents | Own Data | Notes |
|---|---|---|---|
| Person | gość, osoba z obsługi, operator konsoli, ewentualnie właściciel afiliowany | imię, nazwisko | |
| Company | Sieć, wydawca, organizacja nadrzędna, właściciel afiliowany | nazwa | podtypy formy prawnej niepotrzebne |
| OrganizationUnit | kemping, region, rynek, podgrupa wydawcy | nazwa wyświetlana | nazwa nie niesie znaczenia (koniec z prefiksami „AFFILIATE”, „NETWORK ACCESS <MKT>”, „NETWORK CARD”) |

**PartyId:** UUID nadawany w aplikacji, jedna przestrzeń dla wszystkich typów.

#### Registered Identifiers

| Identifier | Value / Validation | Validity | Assignable To |
|---|---|---|---|
| CARD_UID | UID RFID | od–do | Person; w jednej chwili jeden posiadacz; UID przechodzi z gościa na osobę z obsługi przy przygarnięciu |
| MARKET_CODE | HR / IT / FR / ES | brak | jednostka typu rynek |

**Assignment policy:** jedna polityka (wyłączność UID w czasie, bolaczki:215), egzekwowana w `add(identifier, policy)`, ze stałej konfiguracji.

#### Roles

**Strategy:** 2, hierarchia typów: ról jest kilka, są stabilne, a błąd kończy się odmową prądu (Z).

| Role | Form | Eligible Party Types / Constraints | Held By (domain) |
|---|---|---|---|
| ISSUER | globalna; from w ACCEPTANCE_AGREEMENT i CARD_ISSUANCE | Company, jednostka wydawcy | wydawcy, Sieć |
| ACCEPTOR | globalna | Company | partner przyjmujący karty sieci |
| EXCHANGE_PARTNER | w EXCHANGE_PARTNERSHIP (to) | Company z rolą ISSUER | partnerzy wymiany |
| ACCEPTING_SITE / ACCEPTING_REGION | w ACCEPTANCE_AGREEMENT (to) | kemping / region | kempingi, regiony |
| CARDHOLDER | w CARD_ISSUANCE (to) | Person | goście |
| PARENT_ORG / MEMBER_ORG | w ISSUER_MEMBERSHIP | Company / Company albo jednostka | organizacja nadrzędna / podgrupa |
| STAFF_SITE / STAFF_MEMBER | w STAFF_ASSIGNMENT | tylko kemping / Person | kemping / obsługa |
| AFFILIATE_OWNER / OWNED_SITE | w SITE_OWNERSHIP | Company albo Person / kemping | właściciel afiliowany |
| REGION / MARKET | w REGION_MEMBERSHIP, MARKET_OF | jednostka | struktura raportowa |

#### Party Core vs Orbiting Data

**Atomic core:** PartyId, CARD_UID / MARKET_CODE, role, version.

| Data | Decision | Reason |
|---|---|---|
| Zgoda kempingu na przygarnianie kart powitalnych | preferencja przy kempingu, kluczowana PartyId | nie tworzy tożsamości, zmienia się niezależnie |
| Stan karty (blokada) | poza Party | własny rytm (CRD-375) |
| Konto operatora konsoli | poza jądrem | uwierzytelnienie |
| Ustawienia listy lokalnej, flagi słupków | poza Party | sprzęt |

#### Relationships

**Consistency boundary:** autonomiczne PartyRelationship. Reguły dotyczą obu stron, relacji jest dużo (każda karta), sieć ciągle się reorganizuje.

| Relationship | From | To | Validity | Rules (ilościowe / jakościowe / czasowe) | Violation Handling |
|---|---|---|---|---|---|
| ACCEPTANCE_AGREEMENT | ISSUER | kemping albo region | od–do (Z) | brak / from ma rolę ISSUER; atrybut zasięgu: STATIC (kempingi albo region rozwinięty do listy przy zawarciu) albo FOLLOWS_REGION (tylko gdy to = region; obejmuje kempingi będące dziś w regionie, także przyszłe) / brak | prevented |
| ISSUER_MEMBERSHIP | MEMBER_ORG | PARENT_ORG | otwarta | jeden rodzic, bez cykli / obie strony wydawcami / brak; umowy sumują się po ścieżce (AgreementInheritancePolicy = SUM) | prevented |
| CARD_ISSUANCE | ISSUER | CARDHOLDER przez CARD_UID | ważność karty | jeden aktywny wydawca na UID / kart Sieci partner nie przejmuje; partner zmienia tylko swoje / brak | prevented (odrzucenie linii importu) |
| STAFF_ASSIGNMENT | kemping (zawsze konkretny) | Person | od–do (np. miesiąc, CRD-201) | brak / tylko Person, tylko kemping; „region” przy wydaniu rozwijany do listy kempingów / opcjonalne zawężenie do słupków (Z) | prevented |
| SITE_OWNERSHIP | AFFILIATE_OWNER | kemping | otwarta | jeden właściciel / brak / brak | prevented |
| REGION_MEMBERSHIP / MARKET_OF | kemping / region | region / rynek | od–do (historia) | jeden region na kemping / brak / brak; daje prawo do prądu wyłącznie przez umowy FOLLOWS_REGION | prevented |
| EXCHANGE_PARTNERSHIP | Sieć | EXCHANGE_PARTNER | otwarta | kierunki eksport/import / to ma rolę ISSUER / brak | prevented |
| RESPONSIBLE_FOR | Person | jednostka | otwarta | brak | prevented |

Skutki krzyżowe:
- Przeniesienie kempingu zmienia REGION_MEMBERSHIP. Zmienia też prawo do prądu, ale tylko dla wydawców, których umowa jest typu FOLLOWS_REGION na stary albo nowy region. Umowy STATIC, przypisania obsługi i własność zostają bez zmian.
- Zakończenie ISSUER_MEMBERSHIP zabiera podgrupie zasięg z umów nadrzędnych. Dodanie własnej umowy niczego nie zabiera.
- Dołączenie kempingu do regionu nie daje obsłudze regionu nowych kempingów (odpowiedź 3).

#### Permissions in the Context of Other Parties

| Permission | Holder → Context Party | Source | Validity | Used by Decision (+ other data) |
|---|---|---|---|---|
| CARDS_ACCEPTED | posiadacz karty → kemping | CARD_ISSUANCE → ISSUER_MEMBERSHIP\* (suma) → ACCEPTANCE_AGREEMENT (STATIC: kemping na liście; FOLLOWS_REGION: kemping dziś w regionie) | przecięcie ważności relacji | autoryzacja na słupku; potrzebuje też ważności karty, gniazda, budżetu mocy |
| CHARGE_AS_STAFF | osoba z obsługi → kemping | STAFF_ASSIGNMENT, bezpośrednio | z relacji | autoryzacja na słupku |
| RECEIVE_NETWORK_CARDS | partner → wydawca karty | EXCHANGE_PARTNERSHIP(eksport); direct deny dla pary | z relacji / własna | eksport kart |
| SEND_CARDS | partner → Sieć | EXCHANGE_PARTNERSHIP(import); direct deny | z relacji / własna | import kart |
| ACCEPTS_NETWORK_CARDS | kemping afiliowany ← Sieć | ACCEPTANCE_AGREEMENT Sieć → kemping | z relacji | przygarnięcie karty powitalnej, widoczność w aplikacji |

#### Capabilities
Not in scope. Zawężenie do słupków 3–7 to zakres w STAFF_ASSIGNMENT (Z).

#### Questions the Model Answers

| Question | Answered By |
|---|---|
| Czy wydawca karty gościa ma umowę obejmującą kemping słupka? | CARDS_ACCEPTED (ścieżka relacji, suma po członkostwie) |
| Karty których wydawców przyjmuje kemping? | odwrotnie po ACCEPTANCE_AGREEMENT (STATIC i FOLLOWS_REGION dla bieżącego regionu) + w dół po ISSUER_MEMBERSHIP |
| Na których kempingach działają karty wydawcy, własne czy przejęte? | suma umów na ścieżce, każda ze źródłem (własna / od organizacji X) |
| Czy to karta obsługi tego kempingu? | STAFF_ASSIGNMENT |
| Czy kemping afiliowany przyjmuje tylko własne karty, czyj jest? | brak/obecność umowy Sieci; SITE_OWNERSHIP |
| Czy to karta sieci / tego partnera? | CARD_ISSUANCE |
| Czy wolno wymieniać karty wydawcy z partnerem? | RECEIVE_NETWORK_CARDS / SEND_CARDS |
| Kto zyska lub straci prąd po przeniesieniu kempingu? | umowy FOLLOWS_REGION na stary i nowy region (pytanie sieciowe) |

#### Implementation Notes
- Dziś jedno drzewo grup niesie pięć relacji, a znaczenie wynika z nazw grup. Model je rozdziela. Przeniesienie w raportowaniu wpływa na prąd tylko tam, gdzie umowa wprost to deklaruje (FOLLOWS_REGION).
- Polityki: AgreementInheritancePolicy = SUM (stała, odpowiedź 1); zasięg umowy to atrybut każdej umowy (odpowiedź 2); STAFF_ASSIGNMENT tylko do kempingów (odpowiedź 3).
- Zmiana logiki względem dziś: wydawca z własną umową już nie traci umów organizacji nadrzędnej (bolaczki:25 opisuje zastępowanie). Pole „inherited” na ekranie zamienia się w listę umów ze źródłem.
- Rozpoczęcie i zakończenie relacji oraz zmiana REGION_MEMBERSHIP emitują zdarzenia. Odświeżanie list kart słupków zostaje poza Party.
- Implementacja referencyjna: `.claude/skills/archetype-implementer/references/party-impl.md`; moduł https://github.com/Archetypy-Oprogramowania/archetypes/tree/main/party

#### Decyzje

- Wydawca to podmiot z rolą ISSUER — (W) „Mają z siecią umowy akceptacji: ich karty działają na wskazanych kempingach sieci” (business-context.md:75)
- Partner to jeden podmiot w rolach ISSUER i ACCEPTOR — (W) „goście sieci ładują u partnera” (business-context.md:76)
- Umowa akceptacji to relacja wydawca → kemping/region — (W) „zgoda, że karty wydawcy działają na danych kempingach” (business-context.md:148)
- Umowy organizacji nadrzędnej sumują się z umowami podgrupy — (W) odpowiedź użytkownika 1: „A, B i C — umowy się sumują”
- Zasięg umowy to atrybut umowy (STATIC / FOLLOWS_REGION) — (W) odpowiedź użytkownika 2: „umowa na kempingi nie podąża za przeniesieniem, umowa »na region z przyszłymi« podąża”
- Przypisanie obsługi tylko do konkretnych kempingów, z ważnością; region rozwijany przy wydaniu — (W) odpowiedź użytkownika 3
- Karta obsługi tylko na kempingu przypisania — (W) „tylko na jego kempingu, a nie u sąsiada z tego samego regionu” (business-context.md:85)
- Przypisanie z ważnością w czasie — (W) „tam, gdzie są w danym miesiącu przypisane” (business-context.md:86)
- Karta sieci to karta wydana przez Sieć — (W) „czy to karta wydana przez sieć? Jeśli tak, partner nigdy jej nie przejmie” (archetypy.md:65)
- Tryb kempingu afiliowanego to obecność umowy Sieci z kempingiem — (Z) wariant roboczy (archetypy.md:42)
- Właściciel afiliowany jako strona SITE_OWNERSHIP — (Z) wariant roboczy; „(nieznane)” (business-context.md:81)
- Strategia ról 2 — (Z)

#### Założenia

- PartyId to UUID.
- UID karty to identyfikator posiadacza z ważnością. Blokada jest poza Party.
- Pula powitalna to karty wydane przez jednostkę „Rynek X”, bez posiadacza.
- Zgoda na przygarnianie to preferencja kempingu, bez dziedziczenia w górę.
- Zawężenie do słupków to opcjonalny zakres w STAFF_ASSIGNMENT.
- Direct deny przy wymianie, ustawiony dla wydawcy nadrzędnego, obejmuje też jego podgrupy (spójnie z sumą po ścieżce). Łatwo odwrócić.
- Umowa na region bez „przyszłych” jest rozwijana do listy kempingów w chwili zawarcia (STATIC).

#### Pytania i odpowiedzi (FAZA 5b, odpowiedział uczestnik)

1. Wydawca „Adria Cards” ma własną umowę na kemping A, jego organizacja nadrzędna „Euro Cards” na B i C. Gdzie działają karty Adria Cards? → **A, B i C — umowy się sumują** (dziś: tylko A — zmiana zachowania).
2. Wydawca X ma umowę na region „Istria”; Zadar przeniesiony do „Dalmacji”. Czy goście X dalej ładują w Zadarze? → **To zależy od umowy**: atrybut „obejmuje też przyszłe kempingi regionu”.
3. Dozorca z kartą „na region Dalmacja”; do regionu dochodzi Šibenik. Czy ładuje tam bez ograniczeń? → **Nie** — przypisanie obsługi zawsze do konkretnych kempingów (dziś: tak — zmiana zachowania).

### Co i jak

| CO | JAK STARE | JAK W ARCHETYPIE |
|---|---|---|
| Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony. | `ValidationService.isCardAcceptedAt` (`campsite-core/.../card/ValidationService.java:381`) to przelotka do `groupService.isCardAcceptedAt(pedestalId, card)`: karta bez gościa daje false; `extractAllAcceptingSiteGroupIds(guest.group)` bierze najbliższą w linii rodziców niepustą listę umów (wyższe zastępowane) i rozwija całe poddrzewa; `pedestalDao.isInGroupList`. Zwraca `boolean`. | Karta wskazuje osobę, osoba wydawcę, a pytanie brzmi: czy ten wydawca albo któraś organizacja nad nim ma dziś umowę obejmującą kemping słupka. Umowy się sumują, więc własna umowa podgrupy nie zabiera zasięgu z góry. Przeniesienie kempingu zmienia odpowiedź tylko wtedy, gdy umowa jest „na region z przyszłymi kempingami”. Odpowiedź podaje też, która umowa dała zgodę. <details><summary>szczegóły</summary>CARDS_ACCEPTED (posiadacz → kemping) ze ścieżki CARD_ISSUANCE → ISSUER_MEMBERSHIP\* (SUM) → ACCEPTANCE_AGREEMENT. Umowa STATIC: kemping na liście ustalonej przy zawarciu. FOLLOWS_REGION: kemping ma dziś REGION_MEMBERSHIP w regionie umowy. Słupek to rzut na PartyId kempingu. Wynik: tak/nie plus źródło (umowa, czyja, jaki zasięg). Ważność karty, gniazdo i moc zostają w decyzji autoryzacji poza Party.</details> |
| Słupek dostaje listę kart, które ma wpuszczać sam, bez pytania sieci: karty których wydawców przyjmuje kemping tego słupka i które karty personelu mają na niej być? | `LocalCardListService.buildLocalCardList` (`campsite-core/.../card/LocalCardListService.java:138`): przy `includeCards` wywołuje `findIssuerGroupIdsAcceptedAt(grupa słupka)` (rekurencyjny SQL; umowa obejmuje grupę słupka albo przodka), potem `findValidCardsForLocalCardList`; przy `includeHouseCards` wywołuje `getHouseCardsForPedestal` (aktywne karty grupy słupka i wszystkich przodków). Karty personelu idą pierwsze, potem ucinanie. Zwraca `LocalCardList`. | Archetyp daje dwie odwrotne odpowiedzi o kempingu: którzy wydawcy mają dziś z nim umowę (bezpośrednio, przez region „z przyszłymi” albo przez organizację nadrzędną, wtedy wszystkie jej podgrupy) i które osoby z obsługi są do tego kempingu przypisane. Karty obsługi z regionu czy rynku już nie wchodzą automatycznie. Pojemność i kolejność to nie archetyp. <details><summary>szczegóły</summary>Wydawcy: ACCEPTANCE_AGREEMENT z to = kemping (STATIC na liście) lub to = bieżący region kempingu (FOLLOWS_REGION), rozszerzone w dół po ISSUER_MEMBERSHIP (suma). Karty gości to CARD_UID osób w CARD_ISSUANCE z tymi wydawcami. Obsługa: aktywne STAFF_ASSIGNMENT z from = ten kemping, opcjonalnie zawężone do słupków (rzut słupka). Ustawienie LocalList i pojemność: niezmapowane.</details> |
| Gość przyłożył kartę powitalną do słupka i został odrzucony, bo kemping nie przyjmuje tej karty: czy ten niezależny kemping przyjmuje tylko własne karty obsługi i pozwala przygarniać karty powitalne? | `WelcomeCardAdoptionService.checkPedestal` (`campsite-core/.../group/attribute/WelcomeCardAdoptionService.java:133`): `adoptionIsAllowedFor` (najbliższy atrybut w górę, brak to nie), `getAffiliateGroup` (przodek z prefiksem nazwy „AFFILIATE”), `checkAccessMode == HOUSE_CARDS_ONLY` (prefiks nazwy rodzica). Wszystkie trzy muszą być prawdą. Zwraca `boolean`. | Kemping należy do właściciela afiliowanego na mocy relacji własności, a nie z nazwy. „Tylko własne karty” oznacza, że Sieć nie ma z tym kempingiem umowy akceptacji. Zgoda na przygarnianie to ustawienie samego kempingu. Przygarnięcie kończy wydanie karty przez pulę i zaczyna przypisanie posiadacza do obsługi tego konkretnego kempingu. Przeniesienie kempingu niczego z tego nie zmienia, więc przygarnięta karta nie przestaje działać. <details><summary>szczegóły</summary>SITE_OWNERSHIP (AFFILIATE_OWNER → kemping). Tryb to brak ACCEPTANCE_AGREEMENT(Sieć → kemping); przełączenie trybu to rozpoczęcie albo zakończenie tej relacji. Zgoda to preferencja przy kempingu. Karta z puli: CARD_ISSUANCE od jednostki „Rynek X” = rynek kempingu (MARKET_OF ← REGION_MEMBERSHIP). Skutek: koniec CARD_ISSUANCE, STAFF_ASSIGNMENT (ten kemping → osoba z CARD_UID) z ważnością i zdarzenie.</details> |
| Przy wymianie z partnerami sieć wysyła każdemu partnerowi zmiany na liście swoich kart: czy kartę tego gościa wolno przekazać właśnie temu partnerowi? Jeśli nie, partner jej nie dostaje i gość nie naładuje na kempingach partnera. | `IssuerExchangeService.exportCards` (`campsite-core/.../issuer/IssuerExchangeService.java:198`): dla każdego partnera i karty wywołuje `WhitelistFilterService.isSynchronizableForExport(grupa gościa, partner)`. Decyduje najbliższy filtr eksportu w linii rodziców (partner albo wszyscy); brak filtra oznacza tak. Odpowiedź false pomija kartę dla tego partnera. Zwraca `boolean`. | Partner ma z siecią relację wymiany w kierunku eksportu i z niej wynika jego prawo do otrzymywania kart sieci. Wyjątek („temu partnerowi nie wysyłamy kart wydawcy X”) to odebranie prawa dla pary partner–wydawca karty, obejmujące też podgrupy X. Odpowiedź zależy od tego, kto wydał kartę, a nie od miejsca gościa w drzewie. <details><summary>szczegóły</summary>EXCHANGE_PARTNERSHIP (Sieć → partner, eksport) daje RECEIVE_NETWORK_CARDS (partner → wydawca karty). Direct deny dla (partner, wydawca) z ważnością, rozciągnięty w dół po ISSUER_MEMBERSHIP (założenie). Wydawca karty pochodzi z CARD_ISSUANCE. Wysyłka i pliki: niezmapowane.</details> |
| Partner przysyła kartę, którą już znamy: czy to karta sieci? Jeśli tak, nigdy nie przechodzi do wydawcy — zmiana jest odrzucona. | `IssuerCardImportService.isNetworkCard` (`campsite-core/.../issuer/IssuerCardImportService.java:187`), jedyny wołający to `importCard:81`: karta bez grupy gościa daje false, inaczej sprawdza, czy grupa gościa leży w poddrzewie grupy o nazwie „NETWORK CARD”. Zwraca `boolean`. | Karta sieci to karta, którą wydał sam podmiot Sieć. Rozstrzyga wydawca w relacji wydania karty o tym UID, a nie miejsce posiadacza w drzewie ani nazwa grupy. Partner nie może przejąć karty, której nie wydał. <details><summary>szczegóły</summary>CARD_ISSUANCE(from = Sieć) dla CARD_UID. Polityka identyfikatora: jeden aktywny wydawca na UID; zmiana wydawcy przez import jest zabroniona, gdy obecny wydawca to Sieć albo inny partner (to samo pytanie co `checkIssuer`). Karta bez posiadacza to jawny stan „bez wydania”, a nie null.</details> |

### Trzy ficzery z archetypu

1. Umowa akceptacji z datą startu i końca — wydawca i sieć ustalają umowę z wyprzedzeniem, a karty zaczynają działać w dniu otwarcia sezonu i same przestają po wygaśnięciu umowy.
   skąd: „Gdy wydawca dopisuje nowy kemping, chcą, żeby jego goście od razu mogli tam ładować” (business-context.md:76)
   w archetypie: ACCEPTANCE_AGREEMENT ma ważność od–do, a CARDS_ACCEPTED dziedziczy ją z relacji.
2. Prawo do prądu na wskazany dzień, do reklamacji — wsparcie odpowiada, czy karta miała prawo ładować na kempingu w dniu sporu i z jakiej umowy lub przypisania.
   skąd: „Gość z reklamacją z poprzedniego sezonu: do sporu trzeba otworzyć dawne rekordy zużycia” (business-context.md:35)
   w archetypie: relacje (umowa, członkostwo wydawcy, członkostwo w regionie, przypisanie, wydanie karty) mają historię ważności, więc uprawnienie jest wyprowadzane „na dzień D” ze źródłem.
3. Podgląd skutków zerwania umowy organizacji nadrzędnej — zanim operator zdejmie umowę wydawcy międzynarodowego, widzi, które podgrupy i na których kempingach stracą zasięg, bo nie mają tam własnej umowy.
   skąd: „Sieć ma też wydawców międzynarodowych i wydawców z podgrupami” (business-context.md:78)
   w archetypie: pytanie sieciowe po ścieżce ISSUER_MEMBERSHIP → ACCEPTANCE_AGREEMENT (suma); porównanie CARDS_ACCEPTED przed zakończeniem relacji i po nim.

### Pojęcia niezmapowane

słupek, gniazdo, stanowisko; budżet mocy sekcji i zajętość gniazda; sesja poboru; ważność i blokada karty jako cykl życia; lokalna lista kart (pojemność, kolejność, ucinanie, rozsyłka, wersje) i ustawienie LocalList; pula kart powitalnych jako zapas kart na recepcji; mechanika wymiany plików z partnerami; widoczność w aplikacji, zdalne sterowanie, „rozliczany przez kemping”; płatność QR; taryfy i rozliczenia z wydawcami

### Identyfikatory i rzuty

- kemping słupka (rzut słupek → PartyId kempingu): „czy wydawca karty ma umowę obejmującą kemping słupka” (bolaczki:51)
- UID karty: „karta po UID” (bolaczki:133)
- partner wymiany i kierunki: „dla każdego partnera z eksportem” (bolaczki:194)
- rynek słupka do wyboru puli powitalnej: „rynek z grupy słupka (`determineMarketByGroupId`)” (bolaczki:128)

### Sprzeciwy wobec werdyktu

brak

### Otwarte

- Zmiany zachowania względem dziś (odpowiedzi 1 i 3): suma umów zamiast zastępowania; obsługa tylko na kempingach przypisania.
- Założenia (Z): tryb kempingu afiliowanego jako obecność umowy Sieci; właściciel afiliowany jako strona relacji; strategia ról 2.

### Bolączki z adresami w kodzie (wejście pogłębienia)

### BOLĄCZKI Z ADRESAMI W KODZIE

#### B1. Przeniesienie kempingu w drzewie
- co boli: Przeniesienie, scalenie albo zamknięcie kempingu czy regionu w konsoli (technicznie jedno pole rodzica) po cichu zmienia, czyje karty dostają prąd i kto widzi rekordy: gość dostaje odmowę bez powodu, przygarnięta karta przestaje działać, admin rynku traci połowę kempingów i stoi fakturowanie; poprawki są ręczne, bez wyjaśnienia przyczyny.
- kiedy: przeniesienie kempingu do innego regionu (przeciągnięcie w konsoli), także scalenie dwóch kempingów albo dezaktywacja starego regionu
- cel obserwowalny: kemping raportowany w regionie, w którym biznes chce go widzieć, bez zmiany tego, kto na nim ładuje i kto widzi jego rekordy
- zgłoszenia: OPS-202 (#347): po jesiennej reorganizacji admin.it nie widzi przeniesionych kempingów, fakturowanie stoi (otwarte, pilne); CAMP-203 (#3): po przeniesieniu Zadaru pod Dalmację back office nie widzi jego rekordów; „będziemy dalej reorganizować”; CAMP-207 (#76): dezaktywacja starego regionu ukryła rekordy potrzebne do otwartej reklamacji; RPT-212 (#318): dwie osoby z tą samą rolą rynku widzą inne sumy, zależnie od miejsca w drzewie (otwarte); CAMP-212 (#334): karta gościa odrzucana bez powodu kilka razy w tygodniu po przesunięciach; wsparcie nie umie wyjaśnić (otwarte); CAMP-233 (#193): przygarnięta karta powitalna przestaje działać po przeniesieniu kempingu; pomaga ponowne przygarnięcie; OPS-205 (#336): po przeniesieniu raz tak, raz nie wiadomo, kto z obsługi może ładować; operacje chcą reguły albo ostrzeżenia; CAMP-211 (#337): dozorca z uprawnieniem na regionie ładuje też u sąsiada z tego samego regionu; CAMP-219 (#206): po scaleniu dwóch kempingów goście starego B dalej mają odmowę na słupkach A (otwarte); CRD-202 (#198): filtr kart znika po zapisie grupy, częściej na niedawno przeniesionych (wontfix); CAMP-232 (#231): domyślna taryfa regionu raz trafia do nowego kempingu, raz nie, zależnie od głębokości; wiki Konsultacje-2026-03-10 ¶3–4: Zadar: „technicznie to jest jedno pole w bazie […] a potem dzwonią”; widok przywrócony ręcznie; wiki Konsultacje-2026-03-10 ¶13, ¶14, ¶18, ¶22–24: dozorca u sąsiada, przygarnięta karta, scalenie, odmowy „częściej w tygodniach, jak biznes dużo przesuwa”, admin.it; wiki Konsultacje-2026-03-10 ¶26: PO: „żebyśmy mogli poprzesuwać kempingi […] i żeby nic się przy tym nie sypało”

#### B3. Zasięg kart w kilku miejscach
- co boli: Ustalenie, gdzie działa karta, wymaga dotknięcia kilku miejsc: umowa z wydawcą to listy węzłów po obu stronach i filtry na kilku poziomach, karta obsługi działa na całe poddrzewo. Przy nowym kempingu wydawcy zawsze któryś krok się gubi i karty działają w jedną stronę; nie da się dać karty na pięć słupków ani na zmieniający się co miesiąc zestaw kempingów; odmowa nie mówi, który filtr zadziałał.
- kiedy: wydawca podpisuje nowy kemping, dwa kempingi chcą wspólnie ładować, obsłudze trzeba dać kartę na część kempingu albo na kilka kempingów
- zgłoszenia: ISS-205 (#367): dodanie kempingu wydawcy to kilka ręcznych kroków, jeden się gubi i karty działają w jedną stronę (w toku); ISS-203 (#79): karty partnera mają działać od razu na kempingach z umową (wontfix); CAMP-215 (#399): Zadar i sąsiad: goście mają ładować u siebie nawzajem; nikt nie wie, jak to połączyć; CAMP-218 (#72): trzy kempingi Garda: ładowanie wspólne, rozliczenia i raporty osobno; CAMP-210 (#110): pracownik sezonowy tylko na słupkach 3–7, a da się tylko cały kemping; CRD-201 (#345): karta dla ekipy na zestaw kempingów zmieniany co miesiąc (otwarte); ISS-207 (#364): odmowa przez filtr nie mówi, którego poziomu to filtr (otwarte); wiki Konsultacje-2026-03-10 ¶13, ¶15, ¶17–19: sezonowi, ekipy remontowe, filtry, sąsiad na festiwal, Garda, „rytuał na pięć kroków i zawsze któryś się zgubi”; Glossary (acceptance agreement): umowa: karty wydawcy działają na danych kempingach

Bolączki B1 i B3 dotykają tej samej paczki pytań. Pod nimi pytania, dla których zrobiliśmy skrzyżowanie; przy każdym wołającym: miejsce w kodzie (`plik:linia`), CO (co jest robione, w języku problemu), JAK (jak kod robi to dziś) i odpowiedź jednym zdaniem (typ i co znaczy).

##### Paczka pytań: Czy ta karta dostanie prąd na tym kempingu

- zgłoszeń: 6 · dlaczego boli: gość i obsługa dostają odmowę (albo prąd u sąsiada) po zmianie, która miała dotyczyć tylko raportowania; wsparcie nie umie powiedzieć czemu
- co się zmienia: w gnieździe płynie prąd albo nie · kto zauważa: gość przy słupku, personel po odmowie

###### Q1: Czy wydawca karty gościa ma z siecią umowę, że jego karty działają na kempingu, na którym stoi ten słupek?

- **GroupApiService.toAgreementDTO** (`campsite-core/src/main/java/io/pillopl/group/GroupApiService.java:277`) · top-in-island · ścieżka: GroupApiService.toAgreementDTO → GroupServiceHelper.getAcceptanceAgreements · pyta: partly
  - CO: Operator sieci w konsoli ogląda albo zmienia umowy wydawcy kart: na których kempingach działają karty tego wydawcy i czy to jego własna umowa, czy przejęta od organizacji nadrzędnej?
  - JAK: `toAgreementDTO(issuer)` liczy cały zasięg wydawcy, a nie odpowiedź dla jednego słupka: `GroupServiceHelper.getAcceptanceAgreements(issuer, SITE)` (najbliższy na linii rodziców wydawca z niepustą listą umów SITE; listy wyżej są zastępowane, nie łączone), flaga `inherited = !own && !agreements.isEmpty()` (own = wydawca sam ma choć jedną grupę SITE w `getGroups()`), oraz `extractAllAcceptingSiteGroupIds(issuer)` = id całych poddrzew kempingów z umów. Wołane po `findAcceptanceAgreements` (podgląd), `addAcceptanceAgreement` i `removeAcceptanceAgreement` (wynik po zmianie). Pętla po `issuer.getGroups()` nie sprawdza null (helper sprawdza).
  - odpowiedź: `AcceptanceAgreementDTO` — zasięg kart jednego wydawcy: umowy obowiązujące wydawcę (kempingi/regiony wpisane w umowę) i wszystkie kempingi pod nimi

- **GroupService.extractAllAcceptingSiteGroupIds** (`campsite-core/src/main/java/io/pillopl/group/GroupService.java:291`) · top-in-island · ścieżka: GroupService.extractAllAcceptingSiteGroupIds → GroupServiceHelper.extractAllAcceptingSiteGroupIds · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na pytanie: na których kempingach działają karty tego wydawcy?
  - JAK: `GroupService.extractAllAcceptingSiteGroupIds(issuerGroup)` to transakcyjna obwoluta `GroupServiceHelper.extractAllAcceptingSiteGroupIds`; grep po kodzie produkcyjnym nie znajduje wołających (jest bliźniacza `findAcceptingSiteGroupIds` liczona rekurencyjnym SQL). Produkcja woła helper statycznie, z pominięciem tej metody.
  - odpowiedź: `List<Long>` — lista: kempingi (i regiony) pod umowami obowiązującymi wydawcę

- **GroupService.isAcceptedAt** (`campsite-core/src/main/java/io/pillopl/group/GroupService.java:320`) · top-in-island · ścieżka: GroupService.isAcceptedAt → GroupServiceHelper.extractAllAcceptingSiteGroupIds · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na pytanie: czy karty tego wydawcy działają na tym kempingu?
  - JAK: `isAcceptedAt(issuerGroup, siteGroupId)`: null w którymkolwiek argumencie → false; inaczej `GroupServiceHelper.extractAllAcceptingSiteGroupIds(issuerGroup).contains(siteGroupId)`. Brak wołających w kodzie produkcyjnym.
  - odpowiedź: `boolean` — tak: kemping leży pod jedną z umów obowiązujących wydawcę

- **GroupService.isCardAcceptedAt** (`campsite-core/src/main/java/io/pillopl/group/GroupService.java:348`) · top-in-island · ścieżka: GroupService.isCardAcceptedAt · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na to samo pytanie co przy przyłożeniu karty: czy wydawca karty gościa ma umowę obejmującą kemping tego słupka?
  - JAK: Wariant po kodzie słupka: karta bez gościa → false; inaczej `extractAllAcceptingSiteGroupIds(guest.group)` i `pedestalDao.findByCodeAndGroupIds(kod, ids)` (słupek nieskasowany w jednej z grup). Produkcja (ValidationService) woła wariant po id słupka `isCardAcceptedAt(Long, CampingCard)`, ten — nikt.
  - odpowiedź: `boolean` — tak: słupek stoi na kempingu objętym umową wydawcy karty

- **GroupServiceHelper.extractAllAcceptingGroupIds** (`campsite-core/src/main/java/io/pillopl/group/GroupServiceHelper.java:52`) · top-in-island · ścieżka: GroupServiceHelper.extractAllAcceptingGroupIds · pyta: partly
  - CO: Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony.
  - JAK: Ogólny krok „umowy → poddrzewa → id” dla dowolnego typu celu; w produkcji wołany tylko przez `extractAllAcceptingSiteGroupIds` (typ SITE), a ten przez `GroupService.isCardAcceptedAt(Long, CampingCard)` (przyłożenie karty) i `GroupApiService.toAgreementDTO` (ekran umów). Bierze `getAcceptanceAgreements` (najbliższa w linii rodziców niepusta lista umów danego typu), dla każdej umowy `collectSubtreeIds` i składa listę bez powtórzeń; brak umów → pusta lista. Sam nie zna słupka — odpowiada na „gdzie wolno”, nie „czy tu”.
  - odpowiedź: `List<Long>` — lista: kempingi i regiony (z całym ich poddrzewem), na których działają karty wydawcy

- **ValidationService.validateFor** (`campsite-core/src/main/java/io/pillopl/card/ValidationService.java:261`) · top-in-island · ścieżka: ValidationService.validateFor · pyta: yes
  - CO: Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony.
  - JAK: Łańcuch pierwszy-błąd w prywatnym `validateFor`: słupek istnieje → karta personelu (Q4, wczesne ACCEPTED) → znacznik płatności QR → karta znana sieci → jeśli `withoutCardValidation` (dostawa już trwa) tylko czy jest gość, bez pytania o umowę → `isCardAcceptedAt(card, pedestal)`; false → status `CARD_NOT_ACCEPTED_AT_SITE` i powrót; true → ważność karty na chwilę zgłoszenia (`card.validate(remoteDate)` → `translate`). Pytanie pada przy autoryzacji ze słupka (`Authorization.authenticate` przez `validateForNow`), przy starcie dostawy (`SupplyControlUnitImpl`, `validateFor(..., false)`), przy zatrzymaniu inną kartą i przy sprawdzeniu karty w konsoli (`CampingCardService.checkCardAtSocket`).
  - odpowiedź: `boolean` — tak: wydawca karty ma umowę obejmującą kemping słupka

- **ValidationService.isCardAcceptedAt** (`campsite-core/src/main/java/io/pillopl/card/ValidationService.java:381`) · top-in-island · ścieżka: ValidationService.isCardAcceptedAt · pyta: yes
  - CO: Gość przykłada kartę do gniazda na słupku: czy wydawca jego karty ma z siecią umowę obejmującą kemping, na którym stoi słupek? Jeśli nie, prąd nie zostaje włączony.
  - JAK: Przelotka: `groupService.isCardAcceptedAt(pedestal.getId(), card)`. Tam: karta bez gościa → false; `extractAllAcceptingSiteGroupIds(guest.group)`; pusta → false; inaczej `pedestalDao.isInGroupList(pedestalId, ids)` (słupek nieskasowany, którego grupa jest na liście) != null. Wynik wraca do `validateFor`.
  - odpowiedź: `boolean` — tak: słupek stoi na kempingu objętym umową wydawcy karty

###### Q2: Karty których wydawców przyjmuje kemping tego słupka?

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:138`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Słupek dostaje listę kart, które ma wpuszczać sam, bez pytania sieci: karty których wydawców przyjmuje kemping tego słupka? Karty wydawców spoza tej odpowiedzi nie trafiają na słupek.
  - JAK: Gdy atrybut listy lokalnej przyjmuje karty gości (`lla.isIncludeCards()`), `groupService.findIssuerGroupIdsAcceptedAt(pedestal.getGroup())` (rekurencyjny SQL `findGroupIdsAcceptedAt`: dla każdego wydawcy najbliższy w jego linii właściciel listy umów SITE; wydawca przyjęty, gdy umowa obejmuje grupę słupka albo jej przodka) → `campingCardDao.findValidCardsForLocalCardList(ids, dziś)`. Karty personelu idą pierwsze; przy przekroczeniu pojemności karty gości są ucinane. Grupa słupka nie-SITE → `IllegalStateException`, brak grupy → NPE z `requireNonNull` (nie łapane tutaj). Lista idzie na słupek (`sendFull`: rozsyłka automatyczna i ręczna z konsoli) albo do podglądu w konsoli (`getLocalCardList`).
  - odpowiedź: `List<Long>` — lista: wydawcy kart, których umowa (własna albo przejęta od organizacji nadrzędnej) obejmuje kemping słupka

###### Q3: Na których kempingach mogą ładować karty tego wydawcy (tego gościa) i czy to umowa wydawcy, czy odziedziczona po organizacji nadrzędnej?

- **GroupWebService.findAcceptanceAgreements** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:45`) · caller · wejście HTTP · ścieżka: GroupWebService.findAcceptanceAgreements → GroupApiService.findAcceptanceAgreements · pyta: yes
  - CO: Operator sieci w konsoli ogląda mapę organizacji: na których kempingach działają karty tego wydawcy i czy to jego własna umowa, czy przejęta od organizacji nadrzędnej? Wydawcy spoza swojego zakresu operator nie zobaczy.
  - JAK: POST `/findAcceptanceAgreements` → `GroupApi.findAcceptanceAgreements(caller, issuerGroupId)` → `GroupApiService`: `principalResolver.resolve`, `fetchManageable` (grupa musi istnieć i być w zakresie użytkownika w drzewie tej grupy: `isManageable`), `checkType(ISSUER)`, `toAgreementDTO`. Wyjątki tłumaczy `ApiExceptionTranslator`, endpoint opakowuje w `GroupWebServiceFault`. Konsola (`OrganisationPage`) rysuje umowy, przerywaną linią odziedziczone, i podświetla `acceptingSiteGroupIds`.
  - odpowiedź: `FindAcceptanceAgreementsResponse(AcceptanceAgreementDTO)` — zasięg kart wydawcy: obowiązujące umowy i wszystkie kempingi pod nimi

- **PedestalService.findPedestalsForGuest** (`campsite-core/src/main/java/io/pillopl/infrastructure/PedestalService.java:181`) · cited · ścieżka: PedestalService.findPedestalsForGuest · pyta: no
  - CO: Nikt dziś nie pyta tą drogą (poza testami brak wywołań). Odpowiadałaby na pytanie: na których słupkach w moim zakresie może ładować ten gość?
  - JAK: `findPedestalsForGuest(userinfo, guest)`: użytkownik bez grupy SITE albo gość bez wydawcy → pusta lista; inaczej SQL `findPedestalIdsForGuestRestrictedToUserSiteGroup` (poddrzewo SITE użytkownika ∩ słupki pod umowami najbliższego w linii wydawcy właściciela umów). Grep po kodzie produkcyjnym nie znajduje wołających.
  - odpowiedź: `List<Pedestal>` — lista: słupki w zakresie użytkownika, na których działa karta gościa

- **GroupApiService.toAgreementDTO** (`campsite-core/src/main/java/io/pillopl/group/GroupApiService.java:277`) · top-in-island · ścieżka: GroupApiService.toAgreementDTO · pyta: yes
  - CO: Operator sieci w konsoli ogląda albo zmienia umowy wydawcy kart: na których kempingach działają karty tego wydawcy i czy to jego własna umowa, czy przejęta od organizacji nadrzędnej?
  - JAK: `toAgreementDTO(issuer)`: `getAcceptanceAgreements(issuer, SITE)` (najbliższa w linii niepusta lista umów, wyższe zastępowane), `inherited = !own && !agreements.isEmpty()` (own = wydawca ma własną grupę SITE w `getGroups()`, bez sprawdzenia null), `acceptingSiteGroupIds = extractAllAcceptingSiteGroupIds(issuer)` (całe poddrzewa). Wołane po podglądzie oraz po dodaniu i usunięciu umowy.
  - odpowiedź: `AcceptanceAgreementDTO` — zasięg kart jednego wydawcy: obowiązujące umowy i wszystkie kempingi pod nimi

###### Q4: Czy to karta personelu, której wolno ładować na tym kempingu bez ograniczeń (i dla jakiego obszaru ją wydano)?

- **Authorization.authenticate** (`campsite-core/src/main/java/io/pillopl/card/Authorization.java:69`) · cited · ścieżka: Authorization.authenticate · pyta: yes
  - CO: Do gniazda przyłożono kartę personelu kempingu (albo organizacji nad nim): czy włączyć prąd od razu, bez sprawdzania, czy gniazdo jest zajęte i czy sekcja ma zapas mocy?
  - JAK: `authenticate(cardRfid, socketLinkId)` bierze `validationService.validateForNow` (tam `houseCardService.getHouseCardForPedestal` daje wczesne ACCEPTED z `houseCard`). Status != ACCEPTED → odmowa; `validationResult.isHouseCardAccepted()` (houseCard != null) → `houseCardService.recordUse` i ACCEPTED z pominięciem `supplySessionRegistry.hasConcurrentSession` i `sectionPowerBudget.isWithinSectionBudget`, bez tworzenia sesji; inaczej oba sprawdzenia i `createSession`. Wariant po kodzie słupka (101-110) tylko zapisuje użycie.
  - odpowiedź: `ValidationResult` — wynik sprawdzenia karty przy gnieździe; ważne, czy wskazuje kartę personelu

- **HouseCardApiService.saveHouseCard** (`campsite-core/src/main/java/io/pillopl/card/HouseCardApiService.java:50`) · caller · ścieżka: HouseCardApiService.saveHouseCard → HouseCardService.saveHouseCard · pyta: partly
  - CO: Obsługa w konsoli wydaje albo zmienia kartę personelu kempingu: dla jakiego obszaru ta karta ma działać? Zawsze dla kempingu właściciela, w którego imieniu jest wydawana.
  - JAK: Nie pyta, czy karta jest kartą personelu tego słupka — ustala, gdzie później będzie. `HouseCardApiService.saveHouseCard`: uprawnienie `CARD_EDIT`, właściciel, typ, istniejąca karta (inaczej `ObjectNotFoundException`) → `HouseCardService.saveHouseCard`: `checkUserPrivileges`, brak typu/UID → `IllegalArgumentException`, zmieniana karta spoza poddrzewa kempingu właściciela → `AccessDeniedException`, `setGroup(getOwnerSiteGroup(owner))` (właściciel bez kempingu → `IllegalStateException`), zapis i zdarzenie `HOUSE_CARD_CHANGED` (odświeżenie list kart słupków). Wynik mapowany na `HouseCardDTO`.
  - odpowiedź: `HouseCard` — zapisana karta personelu z kempingiem, dla którego działa

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:138`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Słupek dostaje listę kart, które ma wpuszczać sam, bez pytania sieci: które karty personelu (kempingu i organizacji nad nim) mają na niej być? Mają pierwszeństwo przed kartami gości.
  - JAK: Gdy atrybut listy lokalnej przyjmuje karty personelu (`lla.isIncludeHouseCards()`), `houseCardService.getHouseCardsForPedestal(kod)`: nieznany słupek albo bez grupy → pusta lista; inaczej `fetchParentLineIds(grupa słupka, true)` i `houseCardDao.findActiveByGroupIds` (aktywne karty grupy słupka i wszystkich przodków). Karty personelu idą pierwsze; gdy same przekraczają pojemność, karty gości wypadają w całości, a karty personelu są sortowane (ostatnio używane pierwsze) i ucinane.
  - odpowiedź: `List<HouseCard>` — lista: aktywne karty personelu kempingu słupka i organizacji nad nim

- **WelcomeCardAdoptionListener.handleEvent** (`campsite-core/src/main/java/io/pillopl/event/WelcomeCardAdoptionListener.java:37`) · caller · ścieżka: WelcomeCardAdoptionListener.handleEvent → WelcomeCardAdoptionService.checkAndAdopt → HouseCardService.saveHouseCard · pyta: partly
  - CO: Karta odrzucona na kempingu, bo jej wydawca nie ma tam umowy, okazuje się kartą powitalną z puli rynku: czy kemping partnerski przejmuje ją jako swoją kartę personelu? Jeśli tak, od następnego przyłożenia ładuje tam bez ograniczeń.
  - JAK: Zdarzenie `CHECK_WELCOME_CARD_ADOPTION` (wysłane przez `Authorization.checkForCAMP84` przy CARD_NOT_ACCEPTED_AT_SITE) → `welcomeCardAdoptionService.checkAndAdopt(kod słupka, UID)`: słupek nieznany/bez grupy → false; `findWelcomeCard` (karta gościa z grupy-puli kart powitalnych rynku) i `checkPedestal` (adopcja dozwolona atrybutem, jest kemping partnerski po prefiksie nazwy, tryb dostępu HOUSE_CARDS_ONLY) → `houseCardService.saveHouseCard(owner, owner, null, STAFF, …, true)` (karta dla kempingu właściciela partnera) i skasowanie karty powitalnej. Listener ignoruje zwrócony boolean, wyjątki tylko loguje.
  - odpowiedź: `boolean` — tak: karta powitalna przejęta jako karta personelu kempingu partnerskiego, karta gościa skasowana

- **ValidationService.validateFor** (`campsite-core/src/main/java/io/pillopl/card/ValidationService.java:261`) · top-in-island · ścieżka: ValidationService.validateFor · pyta: yes
  - CO: Do gniazda przyłożono kartę: czy to aktywna karta personelu kempingu tego słupka albo organizacji nad nim? Jeśli tak, jest wpuszczana od razu, bez sprawdzania umowy wydawcy i ważności.
  - JAK: W prywatnym `validateFor`, zaraz po sprawdzeniu, że słupek istnieje: `houseCardService.getHouseCardForPedestal(kod, UID)` = pierwsza karta z `getHouseCardsForPedestal` (aktywne karty grupy słupka i przodków) o tym UID, inaczej null. Nie-null → `setHouseCard`, status ACCEPTED, natychmiastowy powrót (przed płatnością QR, kartą gościa, umową i ważnością); null → dalsze sprawdzenia.
  - odpowiedź: `HouseCard albo null` — karta personelu o tym numerze, ważna na tym słupku

###### Q10: Czy kemping chce przejąć kartę powitalną, którą gość zostawił, jako własną kartę obsługi — i czy to w ogóle karta powitalna z puli tego kraju?

- **Authorization.checkForCAMP84** (`campsite-core/src/main/java/io/pillopl/card/Authorization.java:58`) · cited · ścieżka: Authorization.checkForCAMP84 · pyta: partly
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: `Authorization.authenticate` (oba warianty: gniazdo i sam słupek; wołane z protokołów słupka v1–v3 i płatności QR) zaraz po `validateForNow` woła `checkForCAMP84`. Ten pyta tylko o wstępny warunek: status `CARD_NOT_ACCEPTED_AT_SITE` → wysyła `CheckWelcomeCardAdoptionEvent(pedestalCode, cardRfid)`; każdy inny status → nic. Błąd wysłania jest logowany. Właściwe pytanie (pula, zgoda kempingu) zadaje później słuchacz, poza odpowiedzią dla słupka – słupek i tak dostaje odmowę.
  - odpowiedź: `void` — nic nie zwraca

- **CampingCardService.welcomeCardPools** (`campsite-core/src/main/java/io/pillopl/card/CampingCardService.java:235`) · cited · ścieżka: CampingCardService.welcomeCardPools · pyta: partly
  - CO: Recepcja wydaje gościowi wolną kartę powitalną: czy ta karta naprawdę leży w puli kart powitalnych któregoś kraju i czy gość sam nie jest „gościem puli”? Jeśli nie, wydanie karty jest odrzucone.
  - JAK: Prywatne `CampingCardService.welcomeCardPools()` zbiera dla każdego `Market.values()` grupę puli `getExtensionByMarket(market).findWelcomeCardPoolIssuerGroup()` (szukaną po nazwie w poddrzewie wydawców), pomija null. `isWelcomePool(group)` sprawdza, czy grupa jest jedną z nich; `isWelcomePoolCard(card)` – czy grupa gościa karty jest pulą. Klient: `assignWelcomeCard` (konsola, `CardApiService.assignWelcomeCard`): karta nie z puli albo nieaktywowana → `IllegalStateException`; gość należący do puli → `IllegalStateException`. Pyta o pulę dowolnego kraju, nie kraju kempingu – inaczej niż przy przejęciu karty na słupku.
  - odpowiedź: `List<Group>` — lista: pule kart powitalnych wszystkich krajów, które mają taką pulę

- **GroupWebService.getGroupAttributes** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:87`) · caller · wejście HTTP · ścieżka: GroupWebService.getGroupAttributes → GroupApiService.getGroupAttributes · pyta: partly
  - CO: Operator ogląda w konsoli ustawienia kempingu lub regionu: czy to miejsce przejmuje karty powitalne jako własne karty obsługi (własne ustawienie albo odziedziczone z góry)?
  - JAK: `/getGroupAttributes` → `GroupApiService.getGroupAttributes` (sprawdzenie zasięgu `fetchManageable`) → `welcomeCardAdoptionService.adoptionIsAllowedFor(group)`: rekurencyjnie w górę po rodzicach, pierwszy atrybut `WelcomeCardAdoption` decyduje (`isAllowed()`), brak do korzenia → false. Wynik w polu `welcomeCardAdoption`, `OrganisationPage.tsx` pokazuje yes/no. Pokazuje tylko zgodę kempingu – bez warunku „tylko własne karty” i bez puli kraju.
  - odpowiedź: `boolean` — tak: najbliższe ustawienie nad miejscem zgadza się na przejmowanie kart powitalnych

- **WelcomeCardAdoptionListener.handleEvent** (`campsite-core/src/main/java/io/pillopl/event/WelcomeCardAdoptionListener.java:37`) · cited · ścieżka: WelcomeCardAdoptionListener.handleEvent · pyta: yes
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: Słuchacz `CHECK_WELCOME_CARD_ADOPTION` w osobnym zadaniu woła `welcomeCardAdoptionService.checkAndAdopt(pedestalCode, rfid)`: słupek nieznany/bez grupy → false; rynek z grupy słupka (`determineMarketByGroupId`); `findWelcomeCard` (karta gościa z puli tego rynku); `checkPedestal` (zgoda najbliższego przodka + kemping partnerski znaleziony po prefiksie nazwy + tryb `HOUSE_CARDS_ONLY`). Oba tak → `HouseCardService.saveHouseCard(owner, owner, null, STAFF, rfid, numer, true)` dla właściciela z nazwy kempingu i karta powitalna oznaczona jako usunięta. Wynik boolean jest ignorowany, wyjątki logowane.
  - odpowiedź: `boolean` — tak: karta była powitalna z puli kraju, kemping przejmuje takie karty i przyjmuje tylko własne – karta przejęta

- **WelcomeCardAdoptionService.findWelcomeCard** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:114`) · top-in-island · ścieżka: WelcomeCardAdoptionService.findWelcomeCard · pyta: yes
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: `findWelcomeCard(cardRfid, market)` (pakietowo-prywatne, woła je tylko `checkAndAdopt`): brak rynku lub UID → null; pula = `getExtensionByMarket(market).findWelcomeCardPoolIssuerGroup()` (grupa wydawcy szukana po nazwie), brak → null; karta po UID, zwracana tylko gdy jej gość należy dokładnie do grupy puli. Rynek pochodzi z grupy słupka, więc rynek nieustalony (np. zła nazwa grupy) daje null i karta nie zostanie przejęta. `checkAndAdopt` traktuje null jako „nie karta z puli”.
  - odpowiedź: `CampingCard | null` — karta powitalna z puli kraju kempingu o tym UID

- **WelcomeCardAdoptionService.checkPedestal** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:133`) · top-in-island · ścieżka: WelcomeCardAdoptionService.checkPedestal · pyta: yes
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: `checkPedestal(pedestal)` (tylko z `checkAndAdopt`): `adoptionIsAllowedFor(pedestal.getGroup())` – rekurencja po rodzicach, pierwszy atrybut `WelcomeCardAdoption` decyduje, brak → false; `getAffiliateGroup` – pierwszy przodek z nazwą zaczynającą się od prefiksu kempingu partnerskiego; `affiliateGroupService.checkAccessMode(id) == HOUSE_CARDS_ONLY` (tryb wynika z nazwy rodzica, null → false). Wszystkie trzy muszą być prawdą.
  - odpowiedź: `boolean` — tak: kemping partnerski słupka zgadza się na przejmowanie kart powitalnych i przyjmuje tylko własne karty

- **WelcomeCardAdoptionService.getAffiliateGroup** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:149`) · top-in-island · ścieżka: WelcomeCardAdoptionService.getAffiliateGroup · pyta: partly
  - CO: Gość przyłożył do słupka kempingu, który przyjmuje tylko własne karty, kartę, której kemping nie przyjmuje: czy to karta powitalna z puli kraju tego kempingu i czy kemping chce takie karty przejmować? Jeśli tak, karta staje się kartą obsługi kempingu i znika z puli; jeśli nie, zostaje po prostu odrzucona.
  - JAK: Statyczne `getAffiliateGroup(group)`: idzie w górę po rodzicach do pierwszej grupy, której nazwa zaczyna się od `PREFIX_AFFILIATE + " "`; brak → null. Używane w `checkPedestal` (czy słupek w ogóle stoi na kempingu partnerskim, potem tryb dostępu) i w `getAffiliateOwner` (właściciel = reszta nazwy po prefiksie; to on dostaje kartę obsługi w `checkAndAdopt`). Odpowiada na pod-pytanie „który kemping i czyj” – z nazwy grupy.
  - odpowiedź: `Group | null` — kemping partnerski, na którym stoi słupek

###### Q11: Czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi?

- **AffiliatePedestalService.switchAccessMode** (`campsite-core/src/main/java/io/pillopl/infrastructure/AffiliatePedestalService.java:358`) · cited · ścieżka: AffiliatePedestalService.switchAccessMode · pyta: partly
  - CO: Instalator uruchamia słupki u właściciela niezależnego kempingu i ustala, czy kemping ma być otwarty na karty sieci (i widoczny w aplikacji), czy przyjmować tylko własne karty obsługi. Kemping trafia do odpowiedniej części sieci, a jego słupki dostają ustawienia zgodne z wyborem. Uruchomienie nie sprawdza, jaki tryb faktycznie wyszedł.
  - JAK: Prywatny `AffiliatePedestalService.switchAccessMode` woła `AffiliateGroupService.switchAccessMode(grupa, commissioning.getAccessMode(), commissioning.isListedInApp())` na końcu obu wariantów uruchomienia: `commission(List<Pedestal>, installer, token)` (l. 202) i `commission(pedestalCode, …, accessMode)` (l. 247). `AffiliateGroupService.switchAccessMode` przepina rodzica grupy na „NETWORK ACCESS <MKT>” albo „HOUSE CARDS ONLY <MKT>” i ustawia flagi słupków (NETWORK_ACCESS: `listedInApp`=`remoteControl`=wybór; inaczej: `listedInApp`, `remoteControl`, `settledByCampsite` = false), wysyła `AccessModeSwitchedEvent`, po czym odczytuje tryb ponownie przez `checkAccessMode(affiliateGroup)` (prefiks nazwy rodzica). Wynik jest ignorowany, metoda jest `void`; zaraz potem `commissioningService.finish(commissioning)`. Domyślny tryb w `Commissioning` to HOUSE_CARDS_ONLY.
  - odpowiedź: `void` — nic nie zwraca

- **GroupWebService.switchAccessMode** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:69`) · caller · wejście HTTP · ścieżka: GroupWebService.switchAccessMode → GroupApiService.switchAccessMode → AffiliateGroupService.switchAccessMode · pyta: partly
  - CO: Operator w konsoli przełącza niezależny kemping między „otwarty na karty sieci” a „tylko własne karty obsługi” i chce zobaczyć, jaki tryb kemping ma po zmianie. Jeśli kemping nie jest niezależny, leży poza jego zakresem albo tryb nie dał się zmienić – dostaje odmowę.
  - JAK: `GroupWebService.switchAccessMode` (POST /switchAccessMode) → `GroupApiService.switchAccessMode`: `principalResolver.resolve(…, CONFIG_EDIT)`, `fetchManageable` (grupa w zakresie operatora), `ApiValues.parseEnum(AccessMode, …)`, `AffiliateGroupService.isAffiliate(group)` (false → `ConflictException`), potem `AffiliateGroupService.switchAccessMode(group.getId(), targetMode, listedInApp)` – przepięcie rodzica + flagi słupków + `AccessModeSwitchedEvent`, odczyt trybu po zmianie z prefiksu nazwy rodzica. Wynik null → `ConflictException`; inaczej `result.name()` wraca w `SwitchAccessModeResponse`. Wyjątki tłumaczy `ApiExceptionTranslator`, endpoint opakowuje w `GroupWebServiceFault`.
  - odpowiedź: `SwitchAccessModeResponse, throws GroupWebServiceFault` — tryb przyjmowania kart, w jakim kemping jest po zmianie

- **GroupWebService.getGroupAttributes** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:87`) · caller · wejście HTTP · ścieżka: GroupWebService.getGroupAttributes → GroupApiService.getGroupAttributes · pyta: yes
  - CO: Operator ogląda ustawienia kempingu w konsoli: czy ten niezależny kemping jest otwarty na karty sieci, czy przyjmuje tylko własne karty obsługi? Przy kempingu, który nie jest niezależny (albo wisi w części sieci bez trybu), trybu nie widać.
  - JAK: `GroupWebService.getGroupAttributes` (POST /getGroupAttributes) → `GroupApiService.getGroupAttributes`: `fetchManageable`, potem `AffiliateGroupService.isAffiliate(group) ? AffiliateGroupService.checkAccessMode(group) : null` (statyczna wersja, bez logu: patrzy tylko na prefiks nazwy rodzica). Wynik `accessMode.name()` albo null idzie do pola `accessMode` w `GroupAttributesDTO` razem z pozostałymi atrybutami.
  - odpowiedź: `GetGroupAttributesResponse(GroupAttributesDTO), throws GroupWebServiceFault` — tryb przyjmowania kart tego kempingu, jeden z wielu atrybutów na ekranie

- **WelcomeCardAdoptionService.checkPedestal** (`campsite-core/src/main/java/io/pillopl/group/attribute/WelcomeCardAdoptionService.java:133`) · top-in-island · ścieżka: WelcomeCardAdoptionService.checkPedestal · pyta: yes
  - CO: Gość przyłożył kartę powitalną do słupka i został odrzucony, bo kemping nie przyjmuje tej karty: czy ten niezależny kemping przyjmuje tylko własne karty obsługi i pozwala przygarniać karty powitalne? Jeśli tak, karta staje się kartą obsługi kempingu; jeśli nie, zostaje odrzucona i nic się nie zmienia.
  - JAK: Łańcuch: `Authorization.checkForCAMP84` przy statusie `CARD_NOT_ACCEPTED_AT_SITE` wysyła `CheckWelcomeCardAdoptionEvent` → `WelcomeCardAdoptionListener.handleEvent` → `WelcomeCardAdoptionService.checkAndAdopt(pedestalCode, rfid)`. Tam `checkCard(findWelcomeCard(…)) && checkPedestal(pedestal)`. `checkPedestal`: `getAffiliateGroup(pedestal.getGroup())` (pierwsza grupa w górę z nazwą „AFFILIATE …”) i `adoptionIsAllowedFor(grupa słupka)` (najbliższy atrybut adopcji w górę, brak = nie) `&& affiliateGroup != null && HOUSE_CARDS_ONLY == affiliateGroupService.checkAccessMode(affiliateGroup.getId())`. true → `houseCardService.saveHouseCard(owner, …, STAFF, …)` i karta powitalna skasowana (`deleted=true`); false → log, `return false`.
  - odpowiedź: `boolean` — tak: kemping pozwala przygarniać karty powitalne i przyjmuje tylko własne karty obsługi

###### Q12: Czy ten słupek ma pamiętać listę kart, które może przyjąć bez pytania centrali, i jakie karty ma na niej trzymać (karty obsługi, karty gości)?

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:124`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Osoba z obsługi technicznej podgląda, jakie karty ten słupek ma teraz na liście kart przyjmowanych bez pytania centrali (karty obsługi, karty gości) i ile kart się nie zmieściło. Jeśli w tej części sieci słupki nie trzymają listy, widzi pustą listę.
  - JAK: Wołający: `CardApiService.getLocalCardList` (`PEDESTAL_VIEW`, `findPedestalInScope`) → `localCardListService.buildLocalCardList(pedestalCode)` → `CardMapper.toDTO`. Metoda: `getPedestal` (brak/skasowany → `IllegalArgumentException`), `localListAttributeService.getLocalListAttribute(pedestal.getGroup())` (najbliższy atrybut LocalList, tylko gdy `localListAllowed`); null → `LocalCardList.empty(code, getCapacity(pedestal))`; inaczej prywatny `buildLocalCardList(pedestal, lla)`: karty domowe (`includeHouseCards`) + ważne karty wydawców akceptowanych na kempingu (`includeCards`, `groupService.findIssuerGroupIdsAcceptedAt`), przycięte do pojemności sterownika (`getCapacity` z typu słupka). Nic nie jest wysyłane.
  - odpowiedź: `LocalCardList` — lista: karty, które słupek przyjmie bez pytania centrali: najpierw karty obsługi, potem karty gości wydawców przyjmowanych na kempingu

- **LocalCardListService.buildLocalCardList** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:138`) · cited · ścieżka: LocalCardListService.buildLocalCardList · pyta: yes
  - CO: Słupek dostaje od nowa całą listę kart przyjmowanych bez pytania centrali – na żądanie obsługi albo przy rozsyłce list do wszystkich słupków: jakie karty mają na niej być (karty obsługi, karty gości) i które wypadają, gdy brakuje miejsca? Karta, której nie ma na liście, wymaga pytania centrali.
  - JAK: Prywatny `buildLocalCardList(pedestal, lla)` woła `LocalCardListService.sendFull` (l. 81) po sprawdzeniu `getLocalListAttribute` (null → `return false`, bez budowania) oraz publiczna wersja z l. 124. `sendFull` wołają `CardApiService.sendLocalCardList` (`PEDESTAL_EDIT`) i `LocalCardListDistributionService.LocalCardListUpdater.run` (rozsyłka po skasowaniu starej listy). Budowa: `includeHouseCards` → `houseCardService.getHouseCardsForPedestal`; `includeCards` → `campingCardDao.findValidCardsForLocalCardList(groupService.findIssuerGroupIdsAcceptedAt(grupa słupka), dziś)`; ponad pojemność: gdy same karty domowe > pojemność → karty gości zerowane i `sortAndCutHouseCards`, inaczej `cutCards` do reszty miejsca; log ostrzeżenia z liczbą odrzuconych. W `sendFull`: `capacity()==0` → `false`; inaczej `localCardListSender.sendLocalList(pedestal, fullUpdate=true, entries, nextCardListVersion, useCache)`, nieudana wysyłka → `LocalCardListException`.
  - odpowiedź: `LocalCardList` — lista: karty, które słupek ma przyjmować bez pytania centrali: karty obsługi (ostatnio używane najpierw), potem karty gości wydawców przyjmowanych na kempingu (najnowsze najpierw)

- **LocalCardListService.updateLocalListWith** (`campsite-core/src/main/java/io/pillopl/card/LocalCardListService.java:205`) · caller · ścieżka: LocalCardListService.updateLocalListWith → HouseCardService.findPedestalsForHouseCard · pyta: yes
  - CO: Karta obsługi kempingu została dodana, zmieniona albo wyłączona: które słupki trzymają ją na swojej liście kart przyjmowanych bez pytania centrali i muszą dostać tę zmianę? Słupek pominięty dalej przyjmuje (albo odrzuca) tę kartę po staremu.
  - JAK: Łańcuch: `HouseCardService.triggerLocalListUpdate` (po zapisie/zmianie/usunięciu karty domowej, l. 152, 167, 286) → `HouseCardChangedEvent` → `HouseCardChangedListener.handleEvent` → `updateLocalListWith(houseCardId)`. Ta woła `houseCardService.findPedestalsForHouseCard(houseCard)`: najbliższy atrybut LocalList liczony dla grupy KARTY (`houseCard.getGroup()`), null albo `!includeHouseCards` → pusta lista; inaczej słupki z `pedestalDao.findByGroupIds(groupService.findManageable(grupa karty))` filtrowane `isLocalListPedestal` (typ z listą + własny najbliższy atrybut pozwala na listę – bez ponownego sprawdzenia `includeHouseCards` na poziomie słupka); każdy wyjątek → pusta lista. Dla każdego słupka `sendDifferential(code, List.of(houseCard), true)`, błąd → log i dalej.
  - odpowiedź: `List<Pedestal>` — lista: słupki na kempingach właściciela karty (i niżej w sieci), które trzymają listę kart

- **LocalListAttributeService.findNearest** (`campsite-core/src/main/java/io/pillopl/group/attribute/LocalListAttributeService.java:70`) · top-in-island · ścieżka: LocalListAttributeService.findNearest · pyta: yes
  - CO: Czy w tej części sieci (kemping, region, rynek) słupki mają pamiętać listę kart przyjmowanych bez pytania centrali i czy trzymać na niej karty obsługi, karty gości, czy oba rodzaje? Rozstrzyga najbliższe ustawienie nad kempingiem; bez niego słupki listy nie trzymają.
  - JAK: Prywatny `findNearest(groupId)`: `groupService.fetchParentLineIds(groupId, true)` (grupa i jej przodkowie), `localListAttributeDao.findByGroupIds(linia)`, `GroupAttributeHelper.findNearest` – pierwszy atrybut idąc od grupy w górę. Woła go `isLocalListGroup(Long)` (`nearest != null && localListAllowed`) oraz `getLocalListAttribute(Group)` (zwraca atrybut tylko gdy `localListAllowed`, inaczej null). Dalej: `GroupApiService.getGroupAttributes` (pole `localList` na ekranie), `isLocalListPedestal` (wybór słupków dla zmiany karty obsługi), `LocalCardListService.sendFull` / `buildLocalCardList` i `HouseCardService.findPedestalsForHouseCard` (flagi `includeCards`, `includeHouseCards`).
  - odpowiedź: `LocalListAttribute | null` — najbliższe ustawienie listy kart nad kempingiem słupka

###### Q15: Czy karty tego wydawcy wolno wymieniać z tym partnerem — wysyłać mu je i przyjmować od niego?

- **IssuerExchangeService.exportCards** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeService.java:198`) · cited · ścieżka: IssuerExchangeService.exportCards · pyta: yes
  - CO: Przy wymianie z partnerami sieć wysyła każdemu partnerowi zmiany na liście swoich kart: czy kartę tego gościa (z tej części sieci) wolno przekazać właśnie temu partnerowi? Jeśli nie, partner jej nie dostaje i gość nie naładuje na kempingach partnera.
  - JAK: `IssuerExchangeService.exportCards` bierze karty sieci czekające na eksport (`campingCardDao.findCardsForPartnerExport(networkCardGroup)`), dla każdego partnera z eksportem i każdej karty woła `WhitelistFilterService.isSynchronizableForExport(grupa gościa karty, partner)`: najbliższy na linii rodziców filtr eksportowy, który celuje w tego partnera albo we wszystkich (`isTargeting`), decyduje; brak filtra = tak. true → `toCardLine` na liście partnera, false → karta pominięta tylko dla tego partnera. Lista (niepusta) idzie przez `sendCards`; status kierunku CARD_EXPORT zapisany. Karty oznaczane jako wyeksportowane tylko, gdy wszyscy partnerzy dostali listę (także te pominięte przez filtr).
  - odpowiedź: `boolean` — tak: kartę wolno wysłać temu partnerowi — nie ma ustalenia, które tego zabrania (albo najbliższe pozwala)

- **IssuerCardImportService.checkWhitelistSynchronizable** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:149`) · top-in-island · ścieżka: IssuerCardImportService.checkWhitelistSynchronizable · pyta: yes
  - CO: Partner przysyła zmiany na liście swoich kart: czy od tego partnera-wydawcy w ogóle wolno przyjmować karty? Jeśli nie, żadna jego karta nie zostaje przyjęta i jego goście nie naładują na kempingach sieci.
  - JAK: `IssuerCardImportService.importCard` jako pierwszy krok woła prywatne `checkWhitelistSynchronizable(partner)`: dla grupy wydawcy partnera (`partner.getIssuerGroup()`) pyta `WhitelistFilterService.isSynchronizableForImport(groupId, partner)` (najbliższy filtr importowy na linii rodziców celujący w partnera albo we wszystkich; brak = tak). false → `IssuerExchangeException("Issuer '…' is marked as not synchronizable")`. Sprawdzenie idzie dla każdej linii osobno, więc odrzucona jest każda linia z tym samym komunikatem; `importCards` liczy je jako odrzucenia. Partner bez grupy wydawcy — sprawdzenia nie ma.
  - odpowiedź: `void, throws IssuerExchangeException` — nic nie zwraca

- **WhitelistFilterService.findNearest** (`campsite-core/src/main/java/io/pillopl/group/attribute/WhitelistFilterService.java:110`) · top-in-island · ścieżka: WhitelistFilterService.findNearest · pyta: partly
  - CO: Dla danej części sieci (wydawcy): które ustalenie o wymianie kart z partnerami obowiązuje — najbliższe, idąc w górę od tej części, dla danego kierunku i partnera? Samo w sobie nie decyduje „tak/nie”; odpowiada na to, które ustalenie ma głos.
  - JAK: Prywatne `findNearest(issuerGroupId, applies)`: `groupService.fetchParentLineIds(id, true)` (od liścia w górę), potem `GroupAttributeHelper.findNearest(linia, whitelistFilterDao.findByGroupIds(linia), applies)` — pierwszy filtr na linii spełniający predykat (kierunek importu/eksportu i `isTargeting(partner)`). Wołają je cztery publiczne metody: `isSynchronizableForImport/Export(id, partner)` (używane przez import i eksport kart) oraz `getWhitelistFilterForImport`/`exportFilterOf` i jednoargumentowe `isSynchronizableForImport/Export(id)` — te ostatnie bez wywołań w kodzie produkcyjnym (grep).
  - odpowiedź: `WhitelistFilter | null` — ustalenie o wymianie kart, które obowiązuje dla tej części sieci w danym kierunku i dla tego partnera

###### Q16: Czy to karta sieci, czy karta wydana przez partnera — i czy przez tego partnera, który ją teraz przysyła?

- **IssuerExchangeService.importCards** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerExchangeService.java:139`) · caller · ścieżka: IssuerExchangeService.importCards → IssuerCardImportService.importCard · pyta: yes
  - CO: Partner przysyła zmiany na liście swoich kart: czy ta karta to karta sieci albo karta innego wydawcy, której ten partner nie może przejąć ani zmienić? Jeśli tak, ta jedna zmiana jest odrzucona, pozostałe przechodzą.
  - JAK: `IssuerExchangeService.importCards` pobiera linie (`fetchCards`) i dla każdej woła `IssuerCardImportService.importCard(partner, line)`. W środku: karta o tym UID istnieje i `isNetworkCard` → `IssuerExchangeException("CARD_UID='…' exists as a network card already")`; ADD/MODIFY/DELETE na karcie wydawcy (`isIssuerCard`) → `checkIssuer`: grupa gościa karty nie w `findManageable(grupa wydawcy partnera)` → `"…was imported from another issuer already"`; MODIFY/DELETE nieznanej → 'card not found'; ADD nieznanej → nowa karta na gościu partnera. `importCards` łapie każdy `RuntimeException`, liczy `accepted`/`rejections`, zwrot `importCard` ignoruje.
  - odpowiedź: `CampingCard` — nic nie zwraca

- **IssuerCardImportService.checkIssuer** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:160`) · top-in-island · ścieżka: IssuerCardImportService.checkIssuer · pyta: yes
  - CO: Partner przysyła dodanie, zmianę albo usunięcie karty, którą znamy jako kartę wydawcy: czy wydał ją właśnie ten partner? Jeśli nie, zmiana jest odrzucona — karta zostaje przy wydawcy, od którego przyszła wcześniej.
  - JAK: `importCard` woła `checkIssuer(partner, card, uid)` w gałęziach ADD/MODIFY/DELETE, gdy `isIssuerCard(card)`. `checkIssuer`: `groupService.findManageable(partner.getIssuerGroup())` (poddrzewo grupy wydawcy partnera) i `contains(card.getGuest().getGroup().getId())`; brak → `IssuerExchangeException("CARD_UID='…' was imported from another issuer already")`. Uwaga: przesunięcie grupy gościa w drzewie zmienia odpowiedź.
  - odpowiedź: `void, throws IssuerExchangeException` — nic nie zwraca

- **IssuerCardImportService.isNetworkCard** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:187`) · top-in-island · ścieżka: IssuerCardImportService.isNetworkCard · pyta: yes
  - CO: Partner przysyła kartę, którą już znamy: czy to karta sieci? Jeśli tak, nigdy nie przechodzi do wydawcy — zmiana jest odrzucona.
  - JAK: Jedyny wołający: `importCard:81` — `card != null && isNetworkCard(card)` → `IssuerExchangeException("CARD_UID='…' exists as a network card already")`. `isNetworkCard`: bez grupy gościa → false; inaczej `findNetworkCardGroupIds()` (poddrzewo grupy 'NETWORK CARD') zawiera grupę gościa karty.
  - odpowiedź: `boolean` — tak: posiadacz karty leży w części sieci kart sieciowych — to karta sieci

- **IssuerCardImportService.isIssuerCard** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:198`) · top-in-island · ścieżka: IssuerCardImportService.isIssuerCard · pyta: yes
  - CO: Partner przysyła zmianę karty, którą już znamy: czy to karta jakiegoś wydawcy (a nie sieci)? Jeśli tak, wolno ją aktualizować po sprawdzeniu, że to karta tego partnera; jeśli nie, dodanie zakłada nową kartę u partnera, a zmiana lub usunięcie jest odrzucone.
  - JAK: Wołane w `importCard` w trzech gałęziach (95, 110, 125) jako `card != null && isIssuerCard(card)`. `isIssuerCard`: bez grupy gościa → false; `findNetworkCardGroupIds()` null (brak 'NETWORK CARD') → true; inaczej grupa gościa poza poddrzewem kart sieci. ADD + true → `checkIssuer` i aktualizacja; ADD + false → `campingCardService.importCard` na gościu partnera; MODIFY/DELETE + false → 'card not found'.
  - odpowiedź: `boolean` — tak: karta jest kartą któregoś wydawcy

- **IssuerCardImportService.hasGuestGroup** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:206`) · top-in-island · ścieżka: IssuerCardImportService.hasGuestGroup · pyta: partly
  - CO: Czy znana karta ma posiadacza przypisanego do jakiejś części sieci albo wydawcy? Bez tego nie da się powiedzieć, czy to karta sieci, czy wydawcy — karta nie jest traktowana ani jako jedna, ani jako druga.
  - JAK: Prywatne statyczne `hasGuestGroup(card)`: `card != null && card.getGuest() != null && card.getGuest().getGroup() != null`. Wołane tylko z `isNetworkCard` i `isIssuerCard` jako strażnik; false → obie zwracają false.
  - odpowiedź: `boolean` — tak: karta ma posiadacza w strukturze sieci

- **IssuerCardImportService.findNetworkCardGroupIds** (`campsite-core/src/main/java/io/pillopl/issuer/IssuerCardImportService.java:213`) · top-in-island · ścieżka: IssuerCardImportService.findNetworkCardGroupIds · pyta: partly
  - CO: Które części sieci należą do kart sieci (wydanych przez samą sieć)? Od tego zależy, czy przysłaną kartę uznać za kartę sieci, czy wydawcy.
  - JAK: Prywatne `findNetworkCardGroupIds()`: `groupService.fetchNetworkCardIssuerGroup()` (grupa ISSUER o nazwie 'NETWORK CARD'), jeśli jest → `groupService.findManageable(grupa)` (całe poddrzewo), inaczej null. Wołane przy każdym `isNetworkCard`/`isIssuerCard` — czyli kilka razy na linię.
  - odpowiedź: `List<Long> | null` — lista: części sieci (grupy wydawcy) należące do kart sieci — sama grupa kart sieci i wszystko pod nią

###### Q20: Jak jest zorganizowana sieć, za którą odpowiadam: które kempingi należą do których regionów i rynków (a wydawcy i zespoły — do kogo)?

- **GroupTreeFactory.createTree** (`campsite-core/src/main/java/io/pillopl/group/GroupTreeFactory.java:25`) · cited · ścieżka: GroupTreeFactory.createTree · pyta: yes
  - CO: Osoba z obsługi otwiera w konsoli drzewo swojej części sieci (kempingi, regiony i rynki albo wydawcy i zespoły): jak ta część jest zorganizowana, co pod czym leży?
  - JAK: Jedyny wołający: `GroupApiService.getGroupTree` (l.129) z grupą zakresu użytkownika dla danego typu. `createTree(root)`: null → null; inaczej `createNode(root, root.getType(), visited, 0)` — drzewo tylko z grup tego samego typu co korzeń, do 20 poziomów.
  - odpowiedź: `GroupTreeNodeDTO | null` — korzeń: część sieci, za którą odpowiada osoba, z całą strukturą pod nią

- **GroupTreeFactory.createNode** (`campsite-core/src/main/java/io/pillopl/group/GroupTreeFactory.java:32`) · cited · ścieżka: GroupTreeFactory.createNode · pyta: yes
  - CO: Osoba z obsługi otwiera w konsoli drzewo swojej części sieci (kempingi, regiony i rynki albo wydawcy i zespoły): jak ta część jest zorganizowana, co pod czym leży?
  - JAK: Rekurencyjne `createNode(group, type, visited, depth)`: dodaje id do `visited`; jeśli `depth < MAX_DEPTH (20)`, dla każdego dziecka tego samego typu i nieodwiedzonego woła się rekurencyjnie; zwraca `GroupTreeNodeDTO(id, name, displayName, type, children)`. Wołane z `createTree` i z siebie.
  - odpowiedź: `GroupTreeNodeDTO` — jedna część sieci (kemping, region, rynek albo wydawca, zespół) z tym, co pod nią

- **GroupWebService.getGroupTree** (`campsite-endpoints/src/main/java/io/pillopl/webservice/console/group/GroupWebService.java:32`) · cited · wejście HTTP · ścieżka: GroupWebService.getGroupTree · pyta: yes
  - CO: Osoba z obsługi otwiera w konsoli drzewo swojej części sieci (kempingi, regiony i rynki albo wydawcy i zespoły): jak ta część jest zorganizowana, co pod czym leży?
  - JAK: HTTP POST `/getGroupTree` → `getBean().getGroupTree(caller(), request.groupType())` = `GroupApiService.getGroupTree`: `parseEnum(GroupType)` (puste → InvalidInputException), `principal.getGroup(type)` — brak zakresu → `ObjectNotFoundException("User … has no … scope")`; inaczej `GroupTreeFactory.createTree(scope)`. Wynik owinięty w `GetGroupTreeResponse`.
  - odpowiedź: `GetGroupTreeResponse` — część sieci, za którą odpowiada osoba, ze strukturą pod nią

### Implementacja referencyjna

`.claude/skills/archetype-implementer/references/party-impl.md`; moduł repozytorium referencyjnego: https://github.com/Archetypy-Oprogramowania/archetypes/tree/main/party
