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

# ======================================================================== life, grief, work, prisoners (v2)
# %3$s in grief/family lines is the late spouse or the spouse; in gossip lines it is the person talked about,
# %5$s the province, %6$s a second person (bride, captor nation, number of soldiers...).
D.update({
    "work.peasant": [
        ("No job yet. Give me a pickaxe, a hoe, anything - I'm going mad from sitting around.", "Na razie bez pracy. Daj mi kilof, motykę, cokolwiek - wariuję od siedzenia."),
        ("I'm between jobs. Mostly between the bench and the tavern.", "Jestem pomiędzy pracami. Głównie pomiędzy ławką a karczmą."),
        ("Unemployed, %1$s. If you need hands, I have two.", "Bez pracy, %1$s. Jeśli potrzebujesz rąk, mam dwie."),
    ],
    "work.worker": [
        ("Twelve hours at the machines. My ears ring even in my sleep.", "Dwanaście godzin przy maszynach. W uszach dzwoni mi nawet przez sen."),
        ("We make goods. What goods? Goods. Don't ask, just buy.", "Robimy towary. Jakie towary? Towary. Nie pytaj, tylko kupuj."),
        ("Last week the press took a man's hand. The foreman said: production must go on.", "W zeszłym tygodniu prasa urwała komuś dłoń. Brygadzista na to: produkcja musi trwać."),
    ],
    "work.innkeeper": [
        ("I pour beer and listen. Everything in %2$s happens first in my tavern.", "Leję piwo i słucham. Wszystko w kraju najpierw dzieje się w mojej karczmie."),
        ("Half of the weddings in this town started at my counter.", "Połowa ślubów w tym mieście zaczęła się przy moim kontuarze."),
    ],
    "work.doctor": [
        ("Bandages, herbs and a lot of praying. Medicine is mostly praying.", "Bandaże, zioła i dużo modlitwy. Medycyna to głównie modlitwa."),
        ("Bring me the wounded, %1$s. Not the dead - I'm good, but not that good.", "Przynoś mi rannych, %1$s. Nie zmarłych - jestem dobry, ale nie aż tak."),
    ],

    # ------------------------------------------------------------------ grief
    "greet.mourning": [
        ("...Sorry. I'm not myself these days. %3$s...", "...Przepraszam. Nie jestem sobą. %3$s..."),
        ("You came to talk? Few do, now that %3$s is gone.", "Przyszedłeś porozmawiać? Mało kto przychodzi od pogrzebu. %3$s..."),
        ("The house is so quiet without %3$s.", "Dom jest taki cichy. %3$s już nie wróci."),
    ],
    "how.mourning.lava": [
        ("The mine took %3$s. Lava, they said. Nothing left to bury.", "Kopalnia. To ona zabrała najbliższą mi osobę: %3$s. Lawa. Nie było czego pochować."),
        ("Every morning %3$s kissed me and went down the shaft. One morning the shaft spat out fire instead.", "Każdego ranka pocałunek i zejście do szybu. Pewnego ranka szyb wypluł tylko ogień. %3$s..."),
        ("I hate that mine. I hate the iron, the gold, all of it. Give me %3$s back.", "Nienawidzę tej kopalni. Żelaza, złota, wszystkiego. Niech ktoś mi odda moje życie. %3$s..."),
    ],
    "how.mourning.trapped": [
        ("Eight minutes, they said. We dug with our hands. %3$s was knocking on the rock... and then the knocking stopped.", "Dali osiem minut. Kopaliśmy gołymi rękami. Zza skały dochodziło pukanie... a potem ucichło. %3$s..."),
        ("Behind the rubble, alone, hungry, in the dark. That's how %3$s died. Nobody came in time.", "Za gruzem, w samotności, głodzie i ciemności. Tak odeszła najbliższa mi osoba: %3$s. Nikt nie zdążył."),
    ],
    "how.mourning.killed": [
        ("%3$s was killed. War, they say, as if that explains anything.", "%3$s nie żyje. Wojna, mówią, jakby to cokolwiek tłumaczyło."),
        ("Soldiers came. %3$s stood in the door. That's all I can tell you.", "Przyszli żołnierze. W drzwiach stała najbliższa mi osoba. %3$s. Tylko tyle mogę powiedzieć."),
    ],
    "how.mourning.fall": [
        ("%3$s slipped. One step. One stupid step into the dark.", "%3$s. Jedno poślizgnięcie. Jeden głupi krok w ciemność."),
    ],
    "how.mourning.starvation": [
        ("We shared the last bread. %3$s gave me the bigger half. Every time.", "Dzieliliśmy ostatni chleb. %3$s - za każdym razem większa połowa dla mnie."),
    ],
    "how.mourning.accident": [
        ("The machines. They don't stop for anybody. Not even for %3$s.", "Maszyny. Nie zatrzymują się dla nikogo. Nawet dla: %3$s."),
        ("The factory sent a letter and three gold coins. Three. For %3$s.", "Fabryka przysłała list i trzy złote monety. Trzy. Tyle wyceniono życie. %3$s..."),
    ],
    "how.mourning.drowned": [
        ("The water took %3$s. I can't look at the river any more.", "Rzeka. %3$s. Nie mogę już patrzeć na wodę."),
    ],
    "how.mourning.other": [
        ("I lost %3$s. The days are long and grey now.", "Straciłem bliską osobę: %3$s. Dni są teraz długie i szare."),
        ("Please, don't ask. Since %3$s died I just... exist.", "Proszę, nie pytaj. Odkąd odeszła ta osoba - %3$s - po prostu... istnieję."),
    ],
    "gift.mourning": [
        ("Thank you. %3$s used to bring me food like this.", "Dziękuję. Kiedyś dostawałem takie jedzenie co dzień. %3$s..."),
        ("You're kind. I haven't eaten since the funeral.", "Jesteś dobry. Nie jadłem od pogrzebu."),
    ],

    # ------------------------------------------------------------------ trapped
    "greet.trapped": [
        ("HELP! Is someone there? The tunnel collapsed! Get me out!", "POMOCY! Jest tam ktoś? Chodnik się zawalił! Wyciągnijcie mnie!"),
        ("Water... light... anything... how long have I been down here?", "Wody... światła... czegokolwiek... jak długo już tu jestem?"),
        ("Dig, %1$s, dig! I can hear you, I'm right behind the rocks!", "Kop, %1$s, kop! Słyszę cię, jestem tuż za skałami!"),
    ],
    "how.trapped": [
        ("How am I? I'm under a mountain, %1$s! Dig!", "Jak się mam? Siedzę pod górą, %1$s! Kop!"),
        ("Hungry. Cold. Scared. Please tell %3$s I'm alive.", "Głodny. Zmarznięty. Przerażony. Powiedz, proszę, że żyję. %3$s musi wiedzieć."),
    ],
    "family.trapped": [
        ("If I don't get out... tell my family I was thinking of them.", "Jeśli nie wyjdę... powiedz rodzinie, że o nich myślałem."),
    ],

    # ------------------------------------------------------------------ family
    "family.single": [
        ("Family? Me? I'm still looking. The tavern girls laugh at my jokes, that's a start.", "Rodzina? Ja? Wciąż szukam. W karczmie śmieją się z moich żartów, to już coś."),
        ("No wife, no children, no worries. Well, a few worries.", "Bez żony, bez dzieci, bez zmartwień. No, kilka zmartwień jest."),
        ("My mother keeps asking when I'll marry. I keep asking when the famine ends.", "Matka ciągle pyta, kiedy się ożenię. Ja ciągle pytam, kiedy skończy się bieda."),
    ],
    "family.single_f": [
        ("Family? There's a woodcutter who keeps bringing me firewood. We'll see.", "Rodzina? Jest taki drwal, co ciągle przynosi mi drewno. Zobaczymy."),
        ("I'm not in a hurry. Marriage is forever and forever is a long time.", "Nie spieszy mi się. Ślub jest na zawsze, a zawsze to długo."),
    ],
    "family.married_m": [
        ("My wife %3$s runs the house and me. Mostly me.", "Moja żona, %3$s, rządzi domem i mną. Głównie mną."),
        ("%3$s and I share everything: the bed, the soup, the debts.", "%3$s i ja dzielimy wszystko: łóżko, zupę i długi."),
        ("Married life is good, %1$s. Two eat cheaper than one, believe it or not.", "Małżeństwo jest dobre, %1$s. We dwoje je się taniej, wierz albo nie."),
    ],
    "family.married_f": [
        ("My husband %3$s snores like a bear, but he's my bear.", "Mój mąż, %3$s, chrapie jak niedźwiedź, ale to mój niedźwiedź."),
        ("%3$s and I are saving for a bigger house. Someday.", "%3$s i ja oszczędzamy na większy dom. Kiedyś się uda."),
        ("Since the wedding we eat less and laugh more. Love is cheaper than bread.", "Od ślubu jemy mniej i śmiejemy się więcej. Miłość jest tańsza niż chleb."),
    ],
    "family.miner_wife": [
        ("My husband %3$s works in the mine. Every evening I stand at the window until I see his lamp.", "Mój mąż, %3$s, pracuje w kopalni. Co wieczór stoję w oknie, aż zobaczę jego lampę."),
        ("They say the new gallery is deep. Too deep. %3$s just laughs.", "Mówią, że nowy chodnik jest głęboki. Za głęboki. %3$s tylko się śmieje."),
    ],
    "family.miner_husband": [
        ("My wife %3$s went down into the mine. Braver than me, that one.", "Moja żona, %3$s, zjeżdża do kopalni. Odważniejsza ode mnie."),
    ],
    "family.children": [
        ("We have %6$s children now! The house is loud, the pot is empty and I've never been happier.", "Mamy już dzieci: %6$s! W domu głośno, w garnku pusto, a ja nigdy nie byłem szczęśliwszy."),
        ("%6$s little mouths to feed. Lower the taxes, %1$s!", "Małych gąb do wykarmienia: %6$s. Obniż podatki, %1$s!"),
    ],
    "family.spouse_trapped": [
        ("%3$s is trapped in the mine! Please, %1$s, send people with shovels!", "Kopalnia się zawaliła, a tam jest %3$s! Proszę, %1$s, wyślij ludzi z łopatami!"),
        ("I can't eat, I can't sleep. %3$s is down there behind the rubble.", "Nie mogę jeść ani spać. %3$s jest tam, za gruzem."),
    ],
    "family.mourning": [
        ("%3$s is gone. The bed is cold, the table too big.", "%3$s. Już na zawsze. Łóżko jest zimne, stół za duży."),
        ("People say time heals. Time just makes the house quieter. %3$s...", "Ludzie mówią, że czas leczy. Czas tylko wycisza dom. %3$s..."),
    ],
    "family.widowed": [
        ("I was married once. %3$s. Some days I still set two plates.", "Byłem kiedyś w małżeństwie. %3$s. Czasem wciąż nakrywam dla dwojga."),
        ("Life goes on. %3$s would want me to find someone. Maybe.", "Życie toczy się dalej. Może kiedyś znów kogoś pokocham."),
    ],

    # ------------------------------------------------------------------ jobs
    "jobs.ask": [
        ("Work? Now you're talking. What have you got?", "Praca? Teraz rozmawiamy. Co masz?"),
        ("I'm listening, %1$s. Just not the latrines again.", "Słucham, %1$s. Byle nie latryny."),
    ],
    "jobs.none": [
        ("All the places are taken. Build something and come back.", "Wszystkie miejsca zajęte. Zbuduj coś i wróć."),
    ],
    "jobs.quit": [
        ("Fine, I quit. Freedom tastes like... hunger, mostly.", "Dobra, odchodzę. Wolność smakuje jak... głód, głównie."),
        ("No more work? I'll be in the tavern if you need me.", "Koniec pracy? Jakby co, jestem w karczmie."),
    ],
    "jobs.refuse_mourning": [
        ("Not now. Not after %3$s. Give me time.", "Nie teraz. Nie po pogrzebie. %3$s... Daj mi czas."),
        ("I can't even get out of bed. Work can wait. %3$s can't come back.", "Nie mogę nawet wstać z łóżka. Praca poczeka. %3$s nie wróci."),
    ],
    "jobs.refuse_grumpy": [
        ("There? With THOSE people? Never.", "Tam? Z TYMI ludźmi? Nigdy."),
        ("Pay me double and we'll talk. No? Then no.", "Płać podwójnie, to pogadamy. Nie? To nie."),
    ],
    "jobs.accept.any": [
        ("All right, I'll work at the %5$s. Starting tomorrow. Well, today.", "Dobrze, idę pracować (%5$s). Od jutra. No dobra, od dziś."),
    ],
    "jobs.accept.farmer": [
        ("Fields, sun and soil under the nails. I'll take it!", "Pola, słońce i ziemia za paznokciami. Biorę!"),
        ("A farmer! My grandfather would be proud. He'd also say I hold the hoe wrong.", "Rolnik! Dziadek byłby dumny. Powiedziałby też, że źle trzymam motykę."),
    ],
    "jobs.accept.woodcutter": [
        ("Woodcutter? Finally a job where I can hit things.", "Drwal? Nareszcie praca, w której wolno w coś walić."),
        ("Give me the axe. The trees around here won't know what hit them.", "Dawaj siekierę. Tutejsze drzewa nie będą wiedziały, co je trafiło."),
    ],
    "jobs.accept.miner": [
        ("The mine... alright. Somebody has to go down there.", "Kopalnia... no dobrze. Ktoś musi tam schodzić."),
        ("Down into the dark for iron and gold. Pray for me, %1$s.", "W ciemność po żelazo i złoto. Módl się za mnie, %1$s."),
        ("They say the deeper you dig, the richer the ore. And the hotter the lava.", "Mówią, że im głębiej kopiesz, tym bogatsza ruda. I tym gorętsza lawa."),
    ],
    "jobs.accept.smith": [
        ("Hammer and anvil. My kind of music.", "Młot i kowadło. To moja muzyka."),
    ],
    "jobs.accept.merchant": [
        ("Buy cheap, sell dear, smile always. I was born for this.", "Kupuj tanio, sprzedawaj drogo, zawsze się uśmiechaj. Urodziłem się do tego."),
    ],
    "jobs.accept.scholar": [
        ("Books! Quiet! Nobody shouting! When do I start?", "Książki! Cisza! Nikt nie krzyczy! Kiedy zaczynam?"),
    ],
    "jobs.accept.priest": [
        ("I'll serve. Somebody has to pray for all those miners.", "Będę służyć. Ktoś musi się modlić za tych wszystkich górników."),
    ],
    "jobs.accept.worker": [
        ("The factory? Steady pay and a cough for life. Deal.", "Fabryka? Stała pensja i kaszel do końca życia. Umowa stoi."),
        ("Machines don't argue. I like that.", "Maszyny się nie kłócą. Lubię to."),
    ],
    "jobs.accept.innkeeper": [
        ("Pouring beer for a living? Is this a dream?", "Nalewanie piwa jako praca? To sen?"),
    ],
    "jobs.accept.doctor": [
        ("I'll heal them. Or at least make them feel better about dying.", "Będę leczyć. A przynajmniej sprawię, że umieranie będzie mniej straszne."),
    ],
    "recruit.no.married": [
        ("And leave %3$s alone? Find somebody without a family, %1$s.", "I zostawić rodzinę? Nigdy. %3$s czeka na mnie w domu. Znajdź kogoś bez rodziny, %1$s."),
        ("I promised %3$s I'd grow old in this house. No uniform for me.", "Obiecałem, że zestarzejemy się w tym domu. %3$s na mnie czeka. Żadnego munduru."),
    ],

    # ------------------------------------------------------------------ news / gossip
    "news.borders": [
        ("The borders are closed. Soldiers stop everyone at the border posts. Even the goats need papers now.", "Granice są zamknięte. Żołnierze zatrzymują każdego. Nawet kozy potrzebują teraz papierów."),
        ("Since the borders closed, strangers think twice before coming here. Good.", "Odkąd zamknięto granice, obcy dwa razy się zastanowią, zanim tu przyjdą. I dobrze."),
    ],
    "news.prosperity": [
        ("New shoes, new pots, new everything! The factories are working, %1$s.", "Nowe buty, nowe garnki, nowe wszystko! Fabryki pracują, %1$s."),
        ("For the first time in my life I bought something I didn't need. Is this what they call prosperity?", "Pierwszy raz w życiu kupiłem coś, czego nie potrzebuję. To się nazywa dobrobyt?"),
    ],
    "news.gossip.death.lava": [
        ("Did you hear? The mine in %5$s has claimed another life: %3$s. A lava pocket.", "Słyszałeś? Kopalnia (%5$s) pochłonęła kolejną ofiarę: %3$s. Kieszeń lawy."),
        ("They say %3$s broke into a lava pocket. Only a melted pickaxe came back up.", "Mówią, że kilof się stopił, a z szybu wróciło tylko echo. %3$s już nie wyjdzie."),
        ("Poor %3$s. Lava. My cousin says he saw the glow from the shaft.", "Biedny człowiek - %3$s. Lawa. Kuzyn mówi, że widział łunę z szybu."),
    ],
    "news.gossip.death.trapped": [
        ("Have you heard about %3$s? Cave-in. They didn't dig through in time.", "Słyszałeś? Zawał. Ratownicy nie zdążyli. %3$s nie żyje."),
        ("They found %3$s behind the rubble. Starved. In the middle of our country, starved underground.", "Za gruzem znaleźli ciało. %3$s. Śmierć głodowa pod ziemią, w samym środku naszego kraju."),
    ],
    "news.gossip.death.killed": [
        ("%3$s is dead. Killed. These are dark times, %1$s.", "%3$s nie żyje. Śmierć z rąk wroga. Mroczne czasy, %1$s."),
    ],
    "news.gossip.death.fall": [
        ("Did you hear about %3$s? A fall. In the mine, they say.", "Słyszałeś? Śmiertelny upadek w kopalni. %3$s."),
    ],
    "news.gossip.death.starvation": [
        ("Hunger took %3$s last night. Who's next?", "Głód zabrał tej nocy kolejną osobę: %3$s. Kto następny?"),
    ],
    "news.gossip.death.accident": [
        ("An accident at the factory. %3$s. The machine didn't even slow down.", "Wypadek w fabryce. %3$s. Maszyna nawet nie zwolniła."),
        ("They say the factory is cursed. First a hand, now %3$s.", "Mówią, że fabryka jest przeklęta. Najpierw ręka, teraz %3$s."),
    ],
    "news.gossip.death.drowned": [
        ("%3$s drowned. Stay away from deep water, %1$s.", "Utonięcie. %3$s. Trzymaj się z dala od głębokiej wody, %1$s."),
    ],
    "news.gossip.death.other": [
        ("Did you hear? %3$s has died. May they rest in peace.", "Słyszałeś? %3$s nie żyje. Niech spoczywa w pokoju."),
    ],
    "news.gossip.trapped": [
        ("Cave-in in the mine at %5$s! %3$s is down there behind the rubble! Everyone's grabbing shovels!", "Zawał w kopalni (%5$s)! %3$s jest tam, za gruzem! Wszyscy łapią za łopaty!"),
        ("They can still hear %3$s knocking. Somebody has to dig them out before the hunger does its work.", "Wciąż słychać pukanie. %3$s czeka na ratunek. Ktoś musi to odkopać, zanim głód zrobi swoje."),
    ],
    "news.gossip.lost": [
        ("%3$s fell into a cavern under %5$s. Nobody knows where. Somebody has to go down and look!", "%3$s wpada do jaskini pod prowincją %5$s. Nikt nie wie gdzie. Ktoś musi zejść i poszukać!"),
    ],
    "news.gossip.rescued": [
        ("They got %3$s out alive! Thin as a rake, but alive!", "Uratowani! %3$s wraca żywcem spod ziemi! Chudość straszna, ale życie ocalone!"),
        ("A miracle in the mine! %3$s is back. The whole street is drinking to it.", "Cud w kopalni! %3$s wraca do domu. Cała ulica za to pije."),
    ],
    "news.gossip.lava": [
        ("%3$s nearly fell into lava down in the mine! Sealed it with cobblestone at the last moment.", "Mało brakowało, a lawa zalałaby chodnik! W ostatniej chwili zatkano dziurę brukiem. Mówi się, że to %3$s."),
        ("The miners say the deeper galleries are hot. %3$s saw lava with their own eyes.", "Górnicy mówią, że w głębszych chodnikach jest gorąco. Ktoś widział lawę na własne oczy, podobno %3$s."),
    ],
    "news.gossip.marriage": [
        ("%3$s and %6$s got married! The whole street danced till morning.", "Ślub! %3$s i %6$s. Cała ulica tańczyła do rana."),
        ("Have you heard? %3$s and %6$s! Who would have thought.", "Słyszałeś? %3$s i %6$s są małżeństwem! Kto by pomyślał."),
        ("The wedding of %3$s and %6$s - three barrels of beer and only one fight. A success!", "Wesele (%3$s i %6$s) - trzy beczki piwa i tylko jedna bójka. Sukces!"),
    ],
    "news.gossip.birth": [
        ("%3$s and %6$s had a baby! Loud as a war horn already.", "Dziecko! Rodzice: %3$s i %6$s. Już drze się jak róg wojenny."),
    ],
    "news.gossip.surrender": [
        ("The army of %3$s surrendered at %5$s! %6$s soldiers threw down their weapons.", "Armia (%3$s) poddała się pod prowincją %5$s! Żołnierzy, którzy rzucili broń: %6$s."),
        ("Prisoners from %3$s are sitting in %5$s with their hands up. Some of them look relieved.", "Jeńcy (%3$s) siedzą w prowincji %5$s z rękami do góry. Niektórzy wyglądają na zadowolonych."),
    ],
    "news.gossip.execution": [
        ("They say %6$s killed prisoners. %3$s among them. Monsters.", "Mówią, że %6$s zabija jeńców. Wśród ofiar: %3$s. Potwory."),
    ],
    "news.gossip.mercy": [
        ("Our ruler let a prisoner, %3$s, go home to %6$s. Some call it weakness, I call it decency.", "Władca puścił wolno jeńca (%3$s, %6$s). Jedni mówią, że to słabość, ja mówię, że przyzwoitość."),
    ],

    # ------------------------------------------------------------------ prisoners
    "prisoner.greet": [
        ("Don't shoot! I surrender! I have a mother!", "Nie strzelaj! Poddaję się! Mam matkę!"),
        ("My weapons are down, see? What happens to me now, %1$s?", "Broń leży, widzisz? Co teraz ze mną będzie, %1$s?"),
        ("%2$s lost. I lost. Now what?", "%2$s przegrywa. Ja też. I co teraz?"),
    ],
    "prisoner.other": [
        ("I'm a prisoner of %3$s. Talk to them, not to me.", "Jestem jeńcem. Mną rządzi teraz %3$s, nie ja."),
    ],
    "prisoner.yes.army": [
        ("Better a new flag than a grave. I'm yours, commander.", "Lepsza nowa flaga niż grób. Jestem twój, dowódco."),
        ("%2$s never paid me anyway. Where do I sign?", "Moja ojczyzna (%2$s) i tak nigdy mi nie płaciła. Gdzie mam podpisać?"),
    ],
    "prisoner.yes.work": [
        ("No more marching? A bed, a job, a roof? You've got yourself a worker.", "Koniec marszów? Łóżko, praca i dach nad głową? Masz nowego pracownika."),
        ("I'm tired of war. I'll work for you, %1$s.", "Mam dość wojny. Będę dla ciebie pracować, %1$s."),
    ],
    "prisoner.no": [
        ("Never! I'd rather rot here than betray %2$s.", "Nigdy! Wolę tu zgnić, niż zdradzić ojczyznę (%2$s)."),
        ("You can take my sword, not my honour.", "Możesz wziąć mój miecz, ale nie mój honor."),
        ("Ask me again tomorrow. Maybe I'll be hungrier.", "Zapytaj jutro. Może będę bardziej głodny."),
    ],
    "prisoner.flee": [
        ("...He's running! The prisoner is running home!", "...Ucieka! Jeniec ucieka do swoich!"),
    ],
    "prisoner.released": [
        ("I can go? Just like that? Thank you. I won't forget it.", "Mogę iść? Tak po prostu? Dziękuję. Nie zapomnę tego."),
    ],
    "prisoner.full": [
        ("You can't even feed the soldiers you have, %1$s. Your army is full.", "Nie wykarmisz nawet tych, których masz, %1$s. Twoja armia jest pełna."),
    ],
    "prisoner.poor": [
        ("A bribe with empty pockets? Come back when the treasury is full.", "Łapówka z pustą kieszenią? Wróć, gdy skarbiec się zapełni."),
    ],
})
