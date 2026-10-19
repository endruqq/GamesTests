# -*- coding: utf-8 -*-
"""NPC dialog lines: base key -> list of (English, Polish).

Arguments: %1$s player, %2$s speaker's nation, %3$s another nation (enemy/rival/ally),
%4$s speaker's name, %5$s province (or building for news.building).
"""

D = {
    # ------------------------------------------------------------------ greetings
    "greet.famine": [
        ("Please... a crust of bread, %1$s. Anything.", "Proszę... choć skórkę chleba, %1$s. Cokolwiek."),
        ("I boiled my boots yesterday. Today I'm eyeing the laces.", "Wczoraj ugotowałem buty. Dziś łakomie spoglądam na sznurówki."),
        ("Forgive me if I faint mid-sentence. The granaries of %2$s are empty.", "Wybacz, jeśli zemdleję w pół zdania. %2$s ma puste spichlerze."),
        ("We buried three neighbours this week. Hunger doesn't knock before it enters.", "W tym tygodniu pochowaliśmy trzech sąsiadów. Głód nie puka, zanim wejdzie."),
    ],
    "greet.hungry": [
        ("Rations are getting smaller every week. Soon they'll be purely theoretical.", "Racje są coraz mniejsze. Niedługo będą czysto teoretyczne."),
        ("Hello, %1$s. You wouldn't happen to have a turnip on you?", "Witaj, %1$s. Nie masz przypadkiem przy sobie rzepy?"),
        ("My stomach growls louder than the army drums.", "Mój brzuch burczy głośniej niż wojskowe bębny."),
    ],
    "greet.war": [
        ("Keep your voice down, %1$s! Spies of %3$s are everywhere.", "Ciszej, %1$s! Wszędzie kręcą się szpiedzy. Podobno nasyła ich %3$s."),
        ("Is it true the army of %3$s is marching on %5$s?", "Czy to prawda, że %3$s szykuje atak na naszą prowincję?"),
        ("My son went to the front. I sharpen my scythe every evening, just in case.", "Mój syn poszedł na front. Co wieczór ostrzę kosę, tak na wszelki wypadek."),
    ],
    "greet.homeless": [
        ("Sorry about the smell. I sleep in the pigsty. The pigs complained first.", "Wybacz zapach. Śpię w chlewie. Świnie pierwsze złożyły skargę."),
        ("Twelve of us in one hut, %1$s. Somebody's elbow is always in my ear.", "Dwunastu nas w jednej chacie, %1$s. Czyjś łokieć zawsze jest w moim uchu."),
        ("Build more houses, I beg you. I'm tired of sleeping standing up.", "Budujcie więcej domów, błagam. Mam dość spania na stojąco."),
    ],
    "greet.unhappy": [
        ("What do you want? Taxes? Take my last chicken, why don't you.", "Czego? Podatków? To weź od razu moją ostatnią kurę."),
        ("Long live... hm, whoever is in charge this week.", "Niech żyje... hm, ktokolwiek tu w tym tygodniu rządzi."),
        ("Don't mind me, I'm just quietly contemplating a revolution.", "Nie zwracaj na mnie uwagi, po cichu rozważam rewolucję."),
    ],
    "greet.abundant": [
        ("Ah, %1$s! Have a sausage. Have two! The cellars of %2$s are bursting!", "Ach, %1$s! Weź kiełbaskę. Weź dwie! Piwnice pękają w szwach!"),
        ("Three dinners a day and a nap in between. Life in %2$s is good!", "Trzy obiady dziennie i drzemka pomiędzy. %2$s to raj na ziemi!"),
        ("I've put on so much weight my horse filed for retirement.", "Tak przytyłem, że mój koń złożył wniosek o emeryturę."),
    ],
    "greet.happy": [
        ("What a lovely day in %5$s! Even the cows are smiling.", "Co za piękny dzień, %1$s! Nawet krowy się uśmiechają."),
        ("Good day, %1$s! Long live %2$s!", "Dzień dobry, %1$s! Niech żyje %2$s!"),
        ("I'd sing, but my neighbours have suffered enough.", "Zaśpiewałbym, ale sąsiedzi już dość wycierpieli."),
    ],
    "greet.normal": [
        ("Good day to you, %1$s.", "Szczęść Boże, %1$s."),
        ("Name's %4$s. Need something?", "Jestem %4$s. Potrzebujesz czegoś?"),
        ("Another day in %5$s. Same mud, same chickens.", "Kolejny dzień. To samo błoto, te same kury."),
        ("Mind the cow pie, %1$s.", "Uważaj na krowi placek, %1$s."),
    ],
    "greet.foreign": [
        ("A traveller from afar! What brings you to %2$s?", "Podróżny z daleka! Co cię tu sprowadza? Tu rządzi %2$s."),
        ("You talk funny. Not from around here, are you?", "Śmiesznie mówisz. Nie jesteś stąd, co?"),
        ("Welcome to %2$s, stranger. Wipe your boots.", "Witaj, obcy. Tu jest %2$s, więc wytrzyj buty."),
    ],
    "greet.enemy": [
        ("Don't hurt me! I'm just a simple peasant!", "Nie rób mi krzywdy! Jestem tylko prostym chłopem!"),
        ("Invaders! Mother, hide the goat!", "Najeźdźcy! Mamo, chowaj kozę!"),
        ("You won't break the spirit of %2$s! ...Please don't break anything else either.", "%2$s się nie podda! ...Proszę, tylko niczego nie łamcie."),
    ],

    # ------------------------------------------------------------------ how are you
    "how.famine": [
        ("How am I? I ate grass this morning. It tasted like regret.", "Jak się mam? Rano jadłem trawę. Smakowała jak żal."),
        ("My children ask for bread and I tell them stories about bread.", "Dzieci proszą o chleb, a ja opowiadam im bajki o chlebie."),
        ("Old Wojtek died last night. We buried him with his spoon. He would have wanted that.", "Stary Wojtek zmarł w nocy. Pochowaliśmy go z łyżką. Tak by chciał."),
        ("If the ruler of %2$s doesn't build farms soon, there won't be anyone left to tax.", "Jeśli władca szybko nie zbuduje farm, nie będzie już kogo opodatkować."),
    ],
    "how.hungry": [
        ("Hungry, but alive. Mostly.", "Głodny, ale żywy. W większości."),
        ("We're eating the seed grain now. Next year will be... interesting.", "Jemy już ziarno siewne. Przyszły rok będzie... ciekawy."),
        ("Could be worse. Could be raining. Oh wait, it is.", "Mogło być gorzej. Mógłby padać deszcz. A nie, pada."),
    ],
    "how.war": [
        ("I can't sleep. Every night I hear drums from the border with %3$s.", "Nie mogę spać. Co noc słyszę bębny zza granicy. Mówią, że to %3$s."),
        ("They took my horse for the cavalry. I hope he's a general by now.", "Zabrali mi konia do kawalerii. Mam nadzieję, że jest już generałem."),
        ("If %3$s comes here, I've got a pitchfork and a very bad temper.", "Jeśli %3$s tu przyjdzie, mam widły i bardzo zły charakter."),
        ("My brother died defending the tower. Make it count, %1$s.", "Mój brat zginął, broniąc wieży. Niech to nie pójdzie na marne, %1$s."),
    ],
    "how.homeless": [
        ("I sleep under a cart. It's cosy until somebody drives off.", "Śpię pod wozem. Przytulnie, dopóki ktoś nie odjedzie."),
        ("Do you know how cold it is to sleep in a barrel?", "Wiesz, jak zimno się śpi w beczce?"),
        ("The ruler should build houses, not monuments to their own ego.", "Władca powinien budować domy, a nie pomniki własnego ego."),
    ],
    "how.unhappy": [
        ("Taxes, mud and boredom. The holy trinity of %2$s.", "Podatki, błoto i nuda. Nasza święta trójca."),
        ("Some say we should march on the capital with torches. I'm just repeating what some say.", "Niektórzy mówią, żeby iść na stolicę z pochodniami. Ja tylko powtarzam, co mówią."),
        ("Even the priest stopped praying for the ruler. Now he prays for us.", "Nawet ksiądz przestał się modlić za władcę. Teraz modli się za nas."),
    ],
    "how.abundant": [
        ("Splendid! I've eaten so well I can't see my own feet.", "Wspaniale! Tak się najadłem, że nie widzę własnych stóp."),
        ("We have so much bread we've started building with it.", "Mamy tyle chleba, że zaczęliśmy z niego budować."),
        ("I named my new piglet after the ruler. It's a compliment, I swear.", "Nazwałem nowe prosię na cześć władcy. To komplement, przysięgam."),
    ],
    "how.happy": [
        ("Couldn't be better! I got married last week. To the miller's daughter. Or the miller. I was drunk.",
         "Lepiej być nie może! W zeszłym tygodniu się ożeniłem. Z córką młynarza. Albo z młynarzem. Byłem pijany."),
        ("Life is good in %2$s. Even the taxes feel almost voluntary.", "%2$s to dobre miejsce do życia. Nawet podatki są prawie dobrowolne."),
        ("Happy as a pig in mud, %1$s!", "Szczęśliwy jak świnia w błocie, %1$s!"),
    ],
    "how.normal": [
        ("Can't complain. Well, I can, but nobody listens.", "Nie narzekam. To znaczy narzekam, ale nikt nie słucha."),
        ("Same as yesterday. Hopefully same as tomorrow.", "Tak jak wczoraj. Oby tak jak jutro."),
        ("My back hurts, my goat bites, but the harvest was alright.", "Plecy bolą, koza gryzie, ale zbiory były niezłe."),
        ("Fine, thank you. You're the first person to ask in years.", "Dobrze, dziękuję. Jesteś pierwszym od lat, kto pyta."),
    ],
    "how.foreign": [
        ("Fine, until your countrymen start trouble.", "Dobrze, dopóki twoi rodacy nie narobią kłopotów."),
        ("Life in %2$s is quiet. Let's keep it that way, yes?", "%2$s to spokojna kraina. Niech tak zostanie, dobrze?"),
    ],
    "how.enemy": [
        ("How am I? Your people are burning my village! How do you think?!", "Jak się mam? Wasi palą mi wioskę! A jak myślisz?!"),
        ("I was fine until your army showed up.", "Miałem się dobrze, dopóki nie pojawiła się wasza armia."),
    ],

    # ------------------------------------------------------------------ jokes
    "joke.famine": [
        ("Why does everyone in %2$s stand so straight? Nothing in our stomachs to weigh us down.", "Czemu u nas wszyscy mają taką prostą postawę? Nic nie ciąży w brzuchu."),
        ("My diet is going great. Not by choice.", "Moja dieta idzie świetnie. Nie z wyboru."),
        ("What's for dinner? A story about dinner.", "Co na obiad? Opowieść o obiedzie."),
    ],
    "joke.war": [
        ("How many soldiers of %3$s does it take to light a torch? None, they're afraid of the dark.", "Ilu żołnierzy wroga trzeba, żeby zapalić pochodnię? Żadnego, boją się ciemności."),
        ("The army of %3$s is so slow that snails overtake it on the way to battle.", "Wojsko, które wystawia %3$s, jest tak wolne, że ślimaki wyprzedzają je w drodze na bitwę."),
        ("Our general shouted: shields up! So we sold them. Prices were up.", "Generał krzyknął: tarcze w górę! No to sprzedaliśmy. Ceny poszły w górę."),
    ],
    "joke.unhappy": [
        ("What's the difference between our ruler and a creeper? The creeper only blows up your house once.", "Czym różni się nasz władca od creepera? Creeper wysadza ci dom tylko raz."),
        ("Taxes in %2$s are like rain. Constant, cold, and nobody ordered them.", "Nasze podatki są jak deszcz. Ciągłe, zimne i nikt ich nie zamawiał."),
    ],
    "joke.normal": [
        ("Why didn't the skeleton join the army? It had no guts.", "Czemu szkielet nie poszedł do wojska? Nie miał odwagi. Ani flaków."),
        ("A villager walks into a smithy. 'Hmm.' Walks out. 'Hmm.'", "Wieśniak wchodzi do kuźni. 'Hmm.' Wychodzi. 'Hmm.'"),
        ("My wife said I should dig more. So I dug a hole. Now I live in it. She was right.", "Żona powiedziała, żebym więcej kopał. Wykopałem dziurę. Teraz w niej mieszkam. Miała rację."),
        ("Why don't endermen play cards? Someone always looks them in the eye.", "Czemu endermany nie grają w karty? Zawsze ktoś spojrzy im w oczy."),
        ("I asked the librarian for a book about Nether portals. She said it's out of this world.", "Poprosiłem bibliotekarkę o książkę o portalach do Netheru. Powiedziała, że to nie z tego świata."),
    ],
    "joke.abundant": [
        ("We eat so much in %2$s that our sheep went on a diet out of solidarity.", "Jemy tyle, że owce przeszły na dietę z solidarności."),
        ("Our bard ate his lute. It was made of gingerbread. Good times.", "Nasz bard zjadł lutnię. Była z piernika. Piękne czasy."),
    ],

    # ------------------------------------------------------------------ professions
    "work.peasant": [
        ("I do a bit of everything. Mostly mud.", "Robię wszystkiego po trochu. Głównie w błocie."),
        ("Carry this, dig that, feed those. The noble life of a peasant.", "Przynieś to, wykop tamto, nakarm tamte. Szlachetne życie chłopa."),
    ],
    "work.farmer": [
        ("Wheat, carrots, potatoes. And one very suspicious pumpkin.", "Pszenica, marchew, ziemniaki. I jedna bardzo podejrzana dynia."),
        ("If you trample my farmland, I will haunt you.", "Jak zdepczesz mi pole, będę cię straszył po śmierci."),
    ],
    "work.woodcutter": [
        ("I cut trees. I swear the trees have started to bite back.", "Ścinam drzewa. Przysięgam, że drzewa zaczęły mi się odgryzać."),
        ("Logs for the mill, planks for the houses, splinters for me.", "Bale dla tartaku, deski na domy, drzazgi dla mnie."),
    ],
    "work.miner": [
        ("I dig. Down. It's not complicated, but it's honest.", "Kopię. W dół. To nie jest skomplikowane, ale uczciwe."),
        ("Found diamonds once. Then lava found me. Long story.", "Raz znalazłem diamenty. Potem lawa znalazła mnie. Długa historia."),
    ],
    "work.smith": [
        ("Iron goes in, steel comes out, and my eyebrows go missing.", "Żelazo wchodzi, stal wychodzi, a moje brwi znikają."),
        ("The smeltery makes the best steel in %2$s. And the worst air.", "Huta robi najlepszą stal w kraju. I najgorsze powietrze."),
    ],
    "work.merchant": [
        ("Buy low, sell high, and never trust anyone from %3$s.", "Kupuj tanio, sprzedawaj drogo i nigdy nie ufaj kupcom, których przysyła %3$s."),
        ("I sell emeralds, carpets and slightly used horses.", "Sprzedaję szmaragdy, dywany i lekko używane konie."),
    ],
    "work.scholar": [
        ("I'm researching why the sky is blue. Funding is... limited.", "Badam, czemu niebo jest niebieskie. Fundusze są... ograniczone."),
        ("Knowledge is power. It's also very heavy when it's in books.", "Wiedza to potęga. Do tego bardzo ciężka, kiedy jest w książkach."),
    ],
    "work.priest": [
        ("I pray for the harvest, the army and your soul. Mostly the harvest.", "Modlę się za plony, wojsko i twoją duszę. Głównie za plony."),
        ("Ring the bell, light the candles, hope for the best.", "Zadzwonić w dzwon, zapalić świece, liczyć na najlepsze."),
    ],

    # ------------------------------------------------------------------ news
    "news.war": [
        ("They say %2$s and %3$s are at war. In the tavern the odds are three to one.", "Mówią, że %2$s i %3$s są w stanie wojny. W karczmie obstawiają trzy do jednego."),
        ("Soldiers marched through last night and took all our eggs. For morale, they said.", "W nocy przeszło wojsko i zabrało wszystkie jajka. Dla morale, powiedzieli."),
    ],
    "news.famine": [
        ("The neighbours are leaving for other lands. Can't blame them.", "Sąsiedzi wyjeżdżają w inne strony. Trudno ich winić."),
        ("The rats left too. That's when you know it's bad.", "Szczury też się wyniosły. Wtedy wiesz, że jest źle."),
    ],
    "news.rich": [
        ("They say the treasury is full of gold. I've never seen any of it, but it's nice to know.", "Mówią, że skarbiec jest pełen złota. Nigdy go nie widziałem, ale miło wiedzieć."),
        ("Merchants from %3$s come to trade with us. We must be doing something right.", "%3$s przysyła do nas kupców. Musimy coś robić dobrze."),
    ],
    "news.peace": [
        ("Nothing happens here. The last big news was a cow escaping.", "Nic się tu nie dzieje. Ostatnia wielka wiadomość to ucieczka krowy."),
        ("Peace with %3$s holds. For now. Peace is just a deep breath before the shouting.", "Pokój trwa, a %3$s na razie siedzi cicho. Pokój to tylko głęboki wdech przed krzykiem."),
    ],
    "news.lost": [
        ("We lost a province. Grandma says it's the first time since the Great Pig Plague.", "Straciliśmy prowincję. Babcia mówi, że pierwszy raz od Wielkiej Zarazy Świń."),
        ("Refugees keep coming from the border. They have nothing left but stories.", "Z pogranicza wciąż przychodzą uciekinierzy. Zostały im tylko opowieści."),
    ],
    "news.ally": [
        ("We're allied with %3$s now. Their bread is terrible, but their swords are sharp.", "Mamy sojusznika: %3$s. Chleb mają okropny, ale miecze ostre."),
        ("An envoy from %3$s came by. Wore a hat taller than our church.", "Przybył poseł, którego wysłało nam %3$s. Miał kapelusz wyższy niż nasz kościół."),
    ],
    "news.strongest": [
        ("Word is %3$s is the mightiest realm around. Makes you nervous.", "Mówią, że %3$s to najpotężniejsze państwo w okolicy. Człowiek się denerwuje."),
        ("Travellers say %3$s has more soldiers than trees.", "Podróżni mówią, że %3$s ma więcej żołnierzy niż drzew."),
    ],
    "news.building": [
        ("Have you seen our new building, %5$s? Fancy! We'll all be rich now. Probably.", "Widziałeś nasz nowy budynek, %5$s? Elegancko! Teraz wszyscy będziemy bogaci. Chyba."),
        ("They finally finished the %5$s. It took longer than my marriage.", "Wreszcie skończyli: %5$s. Trwało to dłużej niż moje małżeństwo."),
    ],

    # ------------------------------------------------------------------ the ruler
    "ruler.love": [
        ("Our ruler? A saint! Low taxes, full plates. Long may they reign!", "Nasz władca? Święty! Niskie podatki, pełne talerze. Niech panuje wiecznie!"),
        ("I hung a portrait of the ruler above my bed. My wife is jealous.", "Powiesiłem portret władcy nad łóżkiem. Żona jest zazdrosna."),
        ("The best ruler %2$s ever had. Don't tell the previous one.", "Najlepszy władca w historii. Tylko nie mów poprzedniemu."),
    ],
    "ruler.ok": [
        ("Could be worse. The last one fed the treasury to a dragon.", "Mogło być gorzej. Poprzedni karmił skarbem smoka."),
        ("They're alright. Mostly. Sometimes. On Tuesdays.", "Ujdzie. Przeważnie. Czasami. We wtorki."),
        ("As long as there's bread and no war, I don't care who sits on the throne.", "Dopóki jest chleb i nie ma wojny, nie obchodzi mnie, kto siedzi na tronie."),
    ],
    "ruler.hate": [
        ("The ruler? I'd say it to their face, but I like my head where it is.", "Władca? Powiedziałbym mu to w twarz, ale lubię mieć głowę tam, gdzie jest."),
        ("We're starving and they build statues. Statues! Of themselves!", "Głodujemy, a on stawia posągi. Posągi! Samego siebie!"),
        ("There's talk of a new ruler. Maybe a goat. The goat has better ideas.", "Mówi się o nowym władcy. Może o kozie. Koza ma lepsze pomysły."),
    ],

    # ------------------------------------------------------------------ recruitment
    "recruit.yes.normal": [
        ("For %2$s! Where do I get my sword? Is it pointy?", "Niech żyje %2$s! Gdzie dostanę miecz? Czy jest ostry?"),
        ("Fine, fine. Anything to get away from my mother-in-law.", "Dobra, dobra. Wszystko, byle uciec od teściowej."),
    ],
    "recruit.yes.famine": [
        ("Do soldiers get fed? Then I'm your man!", "W wojsku karmią? To jestem twój!"),
        ("I'd fight a dragon for a bowl of soup. Sign me up.", "Za miskę zupy pobiłbym się ze smokiem. Zapisuj mnie."),
    ],
    "recruit.yes.war": [
        ("%3$s burned my father's farm. Give me a spear.", "Wróg spalił farmę mojego ojca. Daj mi włócznię."),
        ("I'll defend %5$s with my life! ...Hopefully not with all of it.", "Będę bronił tej prowincji własnym życiem! ...Oby nie całym."),
    ],
    "recruit.yes.happy": [
        ("Why not! Adventure, glory, a free helmet!", "Czemu nie! Przygoda, chwała, darmowy hełm!"),
    ],
    "recruit.no.normal": [
        ("Me? A soldier? I faint at the sight of jam.", "Ja? Żołnierzem? Mdleję na widok konfitur."),
        ("Sorry, my cow needs me. She's very demanding.", "Wybacz, krowa mnie potrzebuje. Jest bardzo wymagająca."),
    ],
    "recruit.no.happy": [
        ("Why would I go to war? Life is perfect here!", "Po co mam iść na wojnę? Tu jest idealnie!"),
        ("And leave my garden? The tomatoes would never forgive me.", "I zostawić ogródek? Pomidory by mi nie wybaczyły."),
    ],
    "recruit.no.famine": [
        ("I can barely lift a spoon, let alone a sword.", "Ledwo podnoszę łyżkę, a co dopiero miecz."),
    ],
    "recruit.no.war": [
        ("I have six children! Who will feed them if %3$s gets me?", "Mam sześcioro dzieci! Kto je wykarmi, jeśli dopadnie mnie %3$s?"),
    ],
    "recruit.nobarracks": [
        ("Join what army? There aren't even barracks here.", "Do jakiego wojska? Nie ma tu nawet koszar."),
        ("Build barracks first, then we'll talk about glory.", "Najpierw zbuduj koszary, potem pogadamy o chwale."),
    ],
    "recruit.full": [
        ("They say the army is full. Not even room for a drummer.", "Mówią, że wojsko jest pełne. Nie ma miejsca nawet dla dobosza."),
    ],
    "recruit.foreign": [
        ("Join YOUR army? I'm loyal to %2$s, thank you very much.", "Do TWOJEGO wojska? Tu rządzi %2$s i jestem mu wierny, bardzo dziękuję."),
        ("Nice try, stranger.", "Niezła próba, obcy."),
    ],

    # ------------------------------------------------------------------ gifts
    "gift.famine": [
        ("Food! Real food! Bless you, %1$s, bless your children and their goats!", "Jedzenie! Prawdziwe jedzenie! Niech cię Bóg błogosławi, %1$s, ciebie, twoje dzieci i ich kozy!"),
        ("I'll share it with my little ones. They haven't eaten in three days.", "Podzielę się z maluchami. Nie jadły od trzech dni."),
        ("*sobs* I had forgotten what this tastes like.", "*szlocha* Zapomniałem, jak to smakuje."),
    ],
    "gift.normal": [
        ("Oh, thank you! I'll save it for supper.", "Och, dziękuję! Zostawię sobie na kolację."),
        ("How kind! You're nothing like the tax collectors.", "Jak miło! Wcale nie jesteś jak poborcy podatkowi."),
    ],
    "gift.abundant": [
        ("Thanks, but I've eaten seven times today. I'll give it to the pig.", "Dzięki, ale jadłem dziś siedem razy. Dam świni."),
        ("More food? I'll need a bigger belt.", "Jeszcze jedzenie? Będę potrzebował większego pasa."),
    ],
    "gift.foreign": [
        ("A gift from a foreigner? Maybe your people aren't so bad after all.", "Prezent od obcego? Może twoi ludzie nie są jednak tacy źli."),
        ("Thank you, stranger. I'll tell everyone in %2$s about your kindness.", "Dziękuję, obcy. Opowiem wszystkim sąsiadom o twojej dobroci."),
    ],

    # ------------------------------------------------------------------ threats (enemy citizens)
    "threaten.give": [
        ("Take it! Take everything! Just leave my goat!", "Bierz! Bierz wszystko! Tylko zostaw moją kozę!"),
        ("H-here, my savings. Please just go away.", "P-proszę, moje oszczędności. Tylko sobie idź."),
    ],
    "threaten.flee": [
        ("Help! HELP! Invaders!", "Ratunku! RATUNKU! Najeźdźcy!"),
        ("*runs away screaming*", "*ucieka z krzykiem*"),
    ],

    "bye": [
        ("Farewell!", "Bywaj!"),
        ("Mind the mud on your way out.", "Uważaj na błoto po drodze."),
        ("Come back when you have bread.", "Wróć, jak będziesz miał chleb."),
        ("Go with God, %1$s.", "Idź z Bogiem, %1$s."),
    ],

    # ------------------------------------------------------------------ soldiers
    "soldier.greet.normal": [
        ("Reporting for duty, %1$s!", "Melduję się na rozkaz, %1$s!"),
        ("%4$s at your service. Orders?", "%4$s do usług. Rozkazy?"),
        ("All quiet on our front. Too quiet.", "Na naszym froncie spokój. Za spokojnie."),
    ],
    "soldier.greet.war": [
        ("Give the word and we'll march on the enemy!", "Daj tylko znak, a ruszymy na wroga!"),
        ("My blade is sharp and my nerves are... mostly fine.", "Ostrze mam ostre, a nerwy... w większości w porządku."),
        ("For %2$s! Point me at the enemy.", "Niech żyje %2$s! Wskaż mi wroga."),
    ],
    "soldier.greet.famine": [
        ("Commander... can we fight on empty stomachs? Asking for the whole regiment.", "Dowódco... czy da się walczyć na pusty żołądek? Pytam w imieniu całego oddziału."),
        ("I ate my belt. The leather one. The others are eyeing their boots.", "Zjadłem pas. Ten skórzany. Reszta oddziału łypie na buty."),
    ],
    "soldier.greet.unpaid": [
        ("With respect, commander, when do we get paid? Sharpening isn't free.", "Z szacunkiem, dowódco, kiedy żołd? Ostrzenie miecza nie jest za darmo."),
        ("The lads are muttering about deserting. I'm not. Yet.", "Chłopaki szemrzą o dezercji. Ja nie. Jeszcze."),
    ],
    "soldier.greet.happy": [
        ("Morale is excellent, commander! We even started a choir.", "Morale doskonałe, dowódco! Założyliśmy nawet chór."),
        ("Good food, good pay, good weather. Is this a trap?", "Dobre jedzenie, dobry żołd, dobra pogoda. To jakaś pułapka?"),
    ],
    "soldier.follow": [
        ("Right behind you!", "Tuż za tobą!"),
        ("Lead the way, commander.", "Prowadź, dowódco."),
        ("Following. Try not to walk into lava.", "Idę za tobą. Postaraj się nie wejść w lawę."),
    ],
    "soldier.hold": [
        ("I'll hold this position. Nobody passes!", "Utrzymam tę pozycję. Nikt nie przejdzie!"),
        ("Standing guard. Like a very determined tree.", "Stoję na straży. Jak bardzo zdeterminowane drzewo."),
        ("Understood. Not one step back.", "Zrozumiano. Ani kroku w tył."),
    ],
    "soldier.return": [
        ("Back to the barracks. Hope there's soup.", "Wracam do koszar. Oby była zupa."),
        ("Returning to base!", "Wracam do bazy!"),
    ],
    "soldier.dismiss": [
        ("Free at last! I'm going home to my cabbages.", "Wreszcie wolny! Wracam do swojej kapusty."),
        ("An honourable discharge? I'll take it. Farewell, commander!", "Honorowe zwolnienie? Biorę. Bywaj, dowódco!"),
    ],
    "soldier.morale.high": [
        ("We'd follow you into the Nether, commander!", "Poszlibyśmy za tobą do Netheru, dowódco!"),
        ("Spirits are high. Somebody even polished the helmets.", "Duch w narodzie! Ktoś nawet wypolerował hełmy."),
    ],
    "soldier.morale.ok": [
        ("Morale's fine. Food's fine. The sergeant snores, though.", "Morale w porządku. Jedzenie w porządku. Tylko sierżant chrapie."),
        ("We're ready, commander. Mostly.", "Jesteśmy gotowi, dowódco. Przeważnie."),
    ],
    "soldier.morale.low": [
        ("Honestly? The men are tired and scared.", "Szczerze? Ludzie są zmęczeni i przestraszeni."),
        ("Half the regiment wants to go home. The other half already left.", "Połowa oddziału chce do domu. Druga połowa już poszła."),
    ],
    "soldier.morale.famine": [
        ("Morale? We're boiling our shields for soup, commander.", "Morale? Gotujemy tarcze na zupę, dowódco."),
        ("A man can't fight on hope alone. We need bread.", "Na samej nadziei się nie walczy. Potrzebujemy chleba."),
    ],
    "soldier.morale.unpaid": [
        ("No pay for weeks. The men are selling their arrows.", "Od tygodni bez żołdu. Ludzie sprzedają strzały."),
        ("We fight for %2$s... but %2$s should pay us.", "Walczymy za ojczyznę... ale ojczyzna powinna nam płacić."),
    ],
    "soldier.enemy": [
        ("Get lost, dog of %3$s!", "Precz, psie! Wracaj tam, gdzie rządzi %3$s!"),
        ("You'll get nothing from me but cold steel.", "Ode mnie dostaniesz tylko zimną stal."),
        ("%2$s will never kneel!", "%2$s nigdy nie uklęknie!"),
    ],
    "soldier.foreign": [
        ("Move along, citizen of %3$s.", "Przejdź dalej, przybyszu."),
        ("I'm on duty. No chit-chat.", "Jestem na służbie. Bez pogaduszek."),
        ("Nice armour. Is it from %3$s? Ours is better.", "Ładna zbroja. Nasze i tak są lepsze."),
    ],
}
