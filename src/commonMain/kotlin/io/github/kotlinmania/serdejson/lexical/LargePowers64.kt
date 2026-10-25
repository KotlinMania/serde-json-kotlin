package io.github.kotlinmania.serdejson.lexical

// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Precalculated large powers for 64-bit limbs.
/// Large powers (`&[u64]`) for base5 operations.
private val `POW5_1`: SerdeJsonLargePowersArray0 get() = SerdeJsonLargePowersArray0(5uL)

private val `POW5_2`: SerdeJsonLargePowersArray0 get() = SerdeJsonLargePowersArray0(25uL)

private val `POW5_3`: SerdeJsonLargePowersArray0 get() = SerdeJsonLargePowersArray0(625uL)

private val `POW5_4`: SerdeJsonLargePowersArray0 get() = SerdeJsonLargePowersArray0(390625uL)

private val `POW5_5`: SerdeJsonLargePowersArray0 get() = SerdeJsonLargePowersArray0(152587890625uL)

private val `POW5_6`: SerdeJsonLargePowersArray1 get() = SerdeJsonLargePowersArray1(3273344365508751233uL, 1262uL)

private val `POW5_7`: SerdeJsonLargePowersArray2 get() = SerdeJsonLargePowersArray2(7942358959831785217uL, 16807427164405733357uL, 1593091uL)

private val `POW5_8`: SerdeJsonLargePowersArray3 get() = SerdeJsonLargePowersArray3(279109966635548161uL, 2554917779393558781uL, 14124656261812188652uL, 11976055582626787546uL, 2537941837315uL)

private val `POW5_9`: SerdeJsonLargePowersArray4 get() = SerdeJsonLargePowersArray4(13750482914757213185uL, 1302999927698857842uL, 14936872543252795590uL, 2788415840139466767uL, 2095640732773017264uL, 7205570348933370714uL, 7348167152523113408uL, 9285516396840364274uL, 6907659600622710236uL, 349175uL)

private val `POW5_10`: SerdeJsonLargePowersArray5 get() = SerdeJsonLargePowersArray5(8643096425819600897uL, 6743743997439985372uL, 14059704609098336919uL, 10729359125898331411uL, 4933048501514368705uL, 12258131603170554683uL, 2172371001088594721uL, 13569903330219142946uL, 13809142207969578845uL, 16716360519037769646uL, 9631256923806107285uL, 12866941232305103710uL, 1397931361048440292uL, 7619627737732970332uL, 12725409486282665900uL, 11703051443360963910uL, 9947078370803086083uL, 13966287901448440471uL, 121923442132uL)

private val `POW5_11`: SerdeJsonLargePowersArray6 get() = SerdeJsonLargePowersArray6(17679772531488845825uL, 2216509366347768155uL, 1568689219195129479uL, 5511594616325588277uL, 1067709417009240089uL, 9070650952098657518uL, 11515285870634858015uL, 2539561553659505564uL, 17604889300961091799uL, 14511540856854204724uL, 12099083339557485471uL, 7115240299237943815uL, 313979240050606788uL, 10004784664717172195uL, 15570268847930131473uL, 10359715202835930803uL, 17685054012115162812uL, 13183273382855797757uL, 7743260039872919062uL, 9284593436392572926uL, 11105921222066415013uL, 18198799323400703846uL, 16314988383739458320uL, 4387527177871570570uL, 8476708682254672590uL, 4925096874831034057uL, 14075687868072027455uL, 112866656203221926uL, 9852830467773230418uL, 25755239915196746uL, 2201493076310172510uL, 8342165458688466438uL, 13954006576066379050uL, 15193819059903295636uL, 12565616718911389531uL, 3815854855847885129uL, 15696762163583540628uL, 805uL)

private val `POW5_12`: SerdeJsonLargePowersArray7 get() = SerdeJsonLargePowersArray7(16359721904723189761uL, 5323973632697650495uL, 17187956456762001185uL, 3930387638628283780uL, 3374723710406992273uL, 16884225088663222131uL, 10967440051041439154uL, 9686916182456720060uL, 10554548046311730194uL, 7390739362393647554uL, 6316162333127736719uL, 18122464886584070891uL, 4044404959645932768uL, 3801320885861987401uL, 12080950653257274590uL, 16414324262488991299uL, 16395687498836410113uL, 12173633940896186260uL, 10843185433142632150uL, 11048169832730399808uL, 12674828934734683716uL, 17370808310130582550uL, 10500926985433408692uL, 10252725158410704555uL, 14170108270502067523uL, 3698946465517688080uL, 989984870770509463uL, 10965601426733943069uL, 11389898658438335655uL, 6901098232861256586uL, 1921335291173932590uL, 7662788640922083388uL, 9775023833308395430uL, 4640401278902814207uL, 14532050972198413359uL, 8378549018693130223uL, 11672322628395371653uL, 8930704142764178555uL, 6275193859483102017uL, 15782593304269205087uL, 8673060659034172558uL, 8018354414354334043uL, 1824896661540749038uL, 11345563346725559868uL, 14959216444480821949uL, 970189517688324683uL, 3338835207603007873uL, 17684964260791738489uL, 1436466329061721851uL, 4554134986752476101uL, 6398757850768963907uL, 4709779218751158342uL, 10033277748582410264uL, 17932125878679265063uL, 10004750887749091440uL, 256584531835386932uL, 14396282740722731628uL, 3086085133731396950uL, 17831272085689600064uL, 10573926491412564693uL, 14888061047859191737uL, 4570995450261499817uL, 10410165022312935266uL, 5691078631447480790uL, 8632710455805418155uL, 790672778942823293uL, 16505464105756800547uL, 2092171438149740401uL, 17505030673829275878uL, 1291290830058928444uL, 14856191690683232796uL, 8916773426496500052uL, 10152003807578858265uL, 13104441193763861714uL, 649395uL)

private val `POW5_13`: SerdeJsonLargePowersArray8 get() = SerdeJsonLargePowersArray8(kotlin.collections.listOf(15308384451594534913uL, 17913664074042735335uL, 6115977719198531863uL, 5794980608663993169uL, 16544350702855106930uL, 9253787637781258566uL, 4977988951675168190uL, 9087837664087448770uL, 2098480401110016986uL, 15474332540882100712uL, 14042133997396540944uL, 1090855284423485362uL, 12639956485351058381uL, 1454115676006639319uL, 3180465001342538023uL, 14649076551958697729uL, 9801292446545910916uL, 13552201410826594004uL, 6101141927469189381uL, 1881431857880609316uL, 4907847477899433595uL, 8714572486973123228uL, 3514969632331374520uL, 11667642286891470094uL, 2391499697425323350uL, 17486585679659076043uL, 18267223761882105642uL, 2886610765822313148uL, 9302834862968900288uL, 15246507846733637044uL, 15924227519624562840uL, 9743741243284697760uL, 3159780987244964246uL, 7304816812369628428uL, 17584602612559717809uL, 4146812420657846766uL, 14525415362681041515uL, 8477630142371600195uL, 4380695748062263745uL, 12119915994367943173uL, 16970630866565485122uL, 4332724980155264503uL, 8079943140620527639uL, 1687908087554405626uL, 17051081099834002166uL, 12638146269730763230uL, 11883749876933445771uL, 4662462156371383785uL, 4796962238316531176uL, 3325504751659868927uL, 6469595803187862550uL, 5852556621152583005uL, 9229334792448387881uL, 17979733373938620709uL, 13951623534175792756uL, 17075879371091039277uL, 14212246479457938037uL, 4008999959804158260uL, 2414266395366403722uL, 3252733766253918247uL, 6382678985007829216uL, 2245927470982310841uL, 13790724502051307301uL, 13116936866733148041uL, 9718402891306794538uL, 13516274400356104875uL, 17859223875778049403uL, 4396895129099725471uL, 3563053650368467915uL, 12176845952536972668uL, 3492050964335269015uL, 2740656767075170753uL, 4409704077614761919uL, 10237775279597492710uL, 3314206875098230827uL, 16437361028114095448uL, 12361736225407656572uL, 16792510651790145480uL, 11449053143229929935uL, 18336641737580333136uL, 6558939822118891088uL, 4606255756908155300uL, 2360792578991605004uL, 160428430149144538uL, 11644861220729221511uL, 10785178451159739786uL, 14923560618031934681uL, 1902620814992781610uL, 14064076995338910412uL, 11547019064112212657uL, 16847481479966225734uL, 8331994491163145469uL, 11739712981738851885uL, 8008309968651120619uL, 10266969595459035264uL, 15175153381217702033uL, 12208659352573720245uL, 7714061140750342961uL, 2892831567213510541uL, 15453714249045017319uL, 71020323573871677uL, 15431137995750602633uL, 5659146884637671933uL, 5998809010488554503uL, 16552192379299157850uL, 1192197967194298797uL, 16157555793424861524uL, 10929371590994640255uL, 3194469143425738352uL, 6651586784672005225uL, 11062427140788057791uL, 6834443579468668318uL, 16421563197797455922uL, 6251046422506172884uL, 13952303462156793860uL, 16632486601871393224uL, 11313454360291325172uL, 5587835232504462834uL, 3105197524618514637uL, 18268568531031972989uL, 2397205535804309313uL, 59413027864729597uL, 11869878125348715710uL, 12592801707270523266uL, 8070632061321113656uL, 18403647807860650811uL, 267109013517069093uL, 6537214311028855260uL, 5220826919973709902uL, 3448740582779163661uL, 16822239213112884941uL, 5975299384311048185uL, 10294433804430712138uL, 4739856055412448774uL, 12057273038326387897uL, 13119002941950056609uL, 3354445304051737058uL, 13592813067499314594uL, 3890182464434078629uL, 17820384357466425060uL, 9785228118969879380uL, 1778431746734556271uL, 10075313876350055029uL, 13994048489400919028uL, 17948287074199726448uL, 2815088342305858722uL, 2676626035777198370uL, 1174257960026283968uL, 421714788677uL))

private val `POW5_14`: SerdeJsonLargePowersArray9 get() = SerdeJsonLargePowersArray9(kotlin.collections.listOf(11471884475673051137uL, 8902860357476377573uL, 13350296775839230505uL, 10609191786344608888uL, 7261211985859587338uL, 11439672689354862964uL, 16789708072300570627uL, 4607056528866348430uL, 3202978990421512997uL, 2024899620433984146uL, 17666950207239811774uL, 4233228489390288200uL, 9137580478688460738uL, 4060411066587388546uL, 11119949806060600124uL, 867715462473090103uL, 14382394941384869610uL, 4856042377419278489uL, 8265605599571137921uL, 538981667666252469uL, 4270263388700786523uL, 3281140600308898503uL, 4121392524544394174uL, 2077884106245940229uL, 9773041957329767574uL, 7550623316597646685uL, 8611033926449791714uL, 18137922955420802793uL, 2796546741236224013uL, 15477096484628446761uL, 9517540128113714010uL, 9471917970500821378uL, 15938570248662483124uL, 5228016831978462619uL, 15720991252586974501uL, 7662829825220776698uL, 17328310068068434348uL, 3371736428170309730uL, 3803724952191098855uL, 13115926536504376719uL, 16752571196153442257uL, 16540185467776259880uL, 3432518182450051120uL, 5880364967211798870uL, 12355748840305392783uL, 14196090758536469575uL, 7370123524686686319uL, 6819740424617592686uL, 13037938013537368753uL, 15029273671291927100uL, 3671312928327205696uL, 7473228676544792780uL, 17234079691312938123uL, 14164740848093544419uL, 13169904779481875902uL, 7179036968465894054uL, 8244653688947194445uL, 17179797746073799490uL, 5591970751047577674uL, 17530550506268329742uL, 5965746721852312330uL, 1604149463243472865uL, 7734199791463116918uL, 11305790396015856714uL, 4441196105025505137uL, 13046431581185664762uL, 124776524294606713uL, 1134521334706523966uL, 11671728093344476434uL, 14103440020972933148uL, 3966727403013869059uL, 9828094508409132821uL, 4355682486381147287uL, 10261407143988481234uL, 3800455155249557199uL, 12700901937937547500uL, 18184475466894579360uL, 13267691151779895412uL, 4714157123477697445uL, 10770360171308585263uL, 9083344917597998040uL, 12078649873810212155uL, 18218989082046199377uL, 4454285072780637351uL, 5287307245618354742uL, 16042289702059031730uL, 4131926574212754010uL, 217692071448455473uL, 3624845916216282093uL, 2901203491797614218uL, 6679177724033967080uL, 44561358851332790uL, 9094639944041587162uL, 13690915012276084311uL, 1408896670826320686uL, 5359130319612337580uL, 6148412925099835601uL, 5211368532286409612uL, 11386360825549027374uL, 16895182466965795071uL, 3392940493846427241uL, 438089879085393580uL, 4783928372776399972uL, 6278117363595909959uL, 12569481049412674733uL, 15648622492570893902uL, 1966316336235305115uL, 1603775390515993547uL, 13576113010204316709uL, 10821754650102840474uL, 18198222517222903152uL, 6966163076615302988uL, 1373932372410129684uL, 3285839581819684990uL, 30177575069719475uL, 16447047871247307061uL, 11618654126674833808uL, 990072222556306872uL, 1260682336135768017uL, 13862055046689532489uL, 15668483092844698432uL, 1879572630092764264uL, 13912027797058626108uL, 6231679788219816920uL, 13857858054844167403uL, 18101470072534728857uL, 4144579812461609229uL, 7048589655616599284uL, 9946956499532694630uL, 9771303850109874038uL, 6477823708780339765uL, 17526247621747041971uL, 13525995675852669549uL, 3928768291901239810uL, 8094153383078124544uL, 11214278667728965552uL, 11251547162596832610uL, 5964946855123292381uL, 3622548288590237903uL, 13469765967150053587uL, 17798986288523466082uL, 14684592818807932259uL, 16724077276802963921uL, 7119877993753121290uL, 1864571304902781632uL, 12871984921385213812uL, 9065447042604670298uL, 3987130777300360550uL, 6890545752116901685uL, 17275341711601865750uL, 6296474927799264658uL, 1257436973037243463uL, 13854281781965301421uL, 1657132483318662716uL, 17309399540017292849uL, 12808111630089217242uL, 1098489625264462071uL, 14010458905686364135uL, 16134414519481621220uL, 14288255900328821475uL, 3469093466388187882uL, 15982710881468295872uL, 4056765540058056052uL, 15945176389096104089uL, 8625339365793505375uL, 12316179968863788913uL, 15334123773538054321uL, 9536238824220581765uL, 16080825720106203271uL, 6235695225418121745uL, 12035192956458019349uL, 3235835166714703698uL, 5348960676912581218uL, 15315062772709464647uL, 17335089708021308662uL, 16855855317958414409uL, 2369751139431140406uL, 3693542588628609043uL, 7350405893393987577uL, 17402072586341663801uL, 7007897690013647122uL, 15671767872059304758uL, 9259490518292347915uL, 14836045474406130394uL, 4654005815464502513uL, 6487825998330548401uL, 7013356660323385022uL, 7136200343936679946uL, 15341236858676437716uL, 3657357368867197449uL, 12621075530054608378uL, 5603868621997066972uL, 7683447656788439942uL, 450883379216880060uL, 14291494350184945047uL, 5466258454997635048uL, 14206933098432772126uL, 4775870327277641692uL, 1864430798867181939uL, 13748978265070608793uL, 12250822864261576589uL, 12561896977498605296uL, 16060949594257359328uL, 17775189113543311529uL, 11835965177892927035uL, 4218664174878121437uL, 3499000902478111683uL, 15169853304359126294uL, 7076121963053575143uL, 832652347668916805uL, 1292148207755194737uL, 7556838978364207852uL, 5904021986723518500uL, 4610244652288570024uL, 4526508363195533871uL, 746120481022614726uL, 737965197247830486uL, 4006266184415762653uL, 9272188239892688050uL, 15346235246415709678uL, 11850675997347533184uL, 11181059668610842701uL, 6687857983250662774uL, 2908718488661492818uL, 4828337780126983225uL, 18071738646453002184uL, 12790187227727197880uL, 17602483480871623153uL, 12523532189621855977uL, 10598805712727696716uL, 2179787555896149376uL, 2242193929457337594uL, 14908923241136742532uL, 8369182018012550027uL, 13385381554043022324uL, 3332327430110633913uL, 16138090784046208492uL, 16172324607469047339uL, 8279089815915615244uL, 12872906602736235247uL, 10894545290539475621uL, 15428756545851905023uL, 4155747980686992922uL, 4074479178894544043uL, 66083965608603584uL, 13873786284662268377uL, 8861183628277687555uL, 12119497911296021430uL, 2154012318305274287uL, 15490706314503067312uL, 13643145488710608367uL, 672340241093017103uL, 6039493278284091973uL, 9679797700977436461uL, 18070795828318171174uL, 2188146431134935377uL, 5247392385741514952uL, 1852539214842869734uL, 12235621681634112739uL, 8812930319623534062uL, 5585597406294108629uL, 11312989214475901864uL, 1547377291787797995uL, 8641748937186208205uL, 12518148659168623694uL, 6611379197521520985uL, 18096591571068008576uL, 15087021227100112139uL, 13058454842015958418uL, 1473584652966833794uL, 4387660670140018168uL, 8452836916843525402uL, 14376083294443363955uL, 13998026203969090659uL, 611968444648172645uL, 990232438801273845uL, 18001186324715561929uL, 13470591857250177501uL, 14881554140239420091uL, 16696367836720124495uL, 6328076032778459673uL, 17027497695968504616uL, 10192245646262428833uL, 8282482589527318647uL, 4319014353374321425uL, 14134087271041670980uL, 5060230880114618599uL, 13179509240430058600uL, 3903514232614801894uL, 17774749744702165255uL, 15448635507030969726uL, 15983775238358480209uL, 14542832143965487887uL, 9385618098039514666uL, 14431419612662304843uL, 730863073501675978uL, 16750118380379734815uL, 9640uL))

internal val `POW5`: SerdeJsonLargePowersArray10 get() = SerdeJsonLargePowersArray10((`POW5_1`).asSlice(), (`POW5_2`).asSlice(), (`POW5_3`).asSlice(), (`POW5_4`).asSlice(), (`POW5_5`).asSlice(), (`POW5_6`).asSlice(), (`POW5_7`).asSlice(), (`POW5_8`).asSlice(), (`POW5_9`).asSlice(), (`POW5_10`).asSlice(), (`POW5_11`).asSlice(), (`POW5_12`).asSlice(), (`POW5_13`).asSlice(), (`POW5_14`).asSlice())

// Rust fixed array with 1 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray0(element0: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    fun copyValue(): SerdeJsonLargePowersArray0 = SerdeJsonLargePowersArray0(stored0)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0)
}
// Rust fixed array with 2 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray1(element0: kotlin.ULong, element1: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    fun copyValue(): SerdeJsonLargePowersArray1 = SerdeJsonLargePowersArray1(stored0, stored1)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1)
}
// Rust fixed array with 3 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray2(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    fun copyValue(): SerdeJsonLargePowersArray2 = SerdeJsonLargePowersArray2(stored0, stored1, stored2)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2)
}
// Rust fixed array with 5 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray3(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    private val stored3: kotlin.ULong = element3
    val element3: kotlin.ULong get() = stored3
    private val stored4: kotlin.ULong = element4
    val element4: kotlin.ULong get() = stored4
    fun copyValue(): SerdeJsonLargePowersArray3 = SerdeJsonLargePowersArray3(stored0, stored1, stored2, stored3, stored4)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4)
}
// Rust fixed array with 10 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray4(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    private val stored3: kotlin.ULong = element3
    val element3: kotlin.ULong get() = stored3
    private val stored4: kotlin.ULong = element4
    val element4: kotlin.ULong get() = stored4
    private val stored5: kotlin.ULong = element5
    val element5: kotlin.ULong get() = stored5
    private val stored6: kotlin.ULong = element6
    val element6: kotlin.ULong get() = stored6
    private val stored7: kotlin.ULong = element7
    val element7: kotlin.ULong get() = stored7
    private val stored8: kotlin.ULong = element8
    val element8: kotlin.ULong get() = stored8
    private val stored9: kotlin.ULong = element9
    val element9: kotlin.ULong get() = stored9
    fun copyValue(): SerdeJsonLargePowersArray4 = SerdeJsonLargePowersArray4(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9)
}
// Rust fixed array with 19 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray5(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong, element10: kotlin.ULong, element11: kotlin.ULong, element12: kotlin.ULong, element13: kotlin.ULong, element14: kotlin.ULong, element15: kotlin.ULong, element16: kotlin.ULong, element17: kotlin.ULong, element18: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    private val stored3: kotlin.ULong = element3
    val element3: kotlin.ULong get() = stored3
    private val stored4: kotlin.ULong = element4
    val element4: kotlin.ULong get() = stored4
    private val stored5: kotlin.ULong = element5
    val element5: kotlin.ULong get() = stored5
    private val stored6: kotlin.ULong = element6
    val element6: kotlin.ULong get() = stored6
    private val stored7: kotlin.ULong = element7
    val element7: kotlin.ULong get() = stored7
    private val stored8: kotlin.ULong = element8
    val element8: kotlin.ULong get() = stored8
    private val stored9: kotlin.ULong = element9
    val element9: kotlin.ULong get() = stored9
    private val stored10: kotlin.ULong = element10
    val element10: kotlin.ULong get() = stored10
    private val stored11: kotlin.ULong = element11
    val element11: kotlin.ULong get() = stored11
    private val stored12: kotlin.ULong = element12
    val element12: kotlin.ULong get() = stored12
    private val stored13: kotlin.ULong = element13
    val element13: kotlin.ULong get() = stored13
    private val stored14: kotlin.ULong = element14
    val element14: kotlin.ULong get() = stored14
    private val stored15: kotlin.ULong = element15
    val element15: kotlin.ULong get() = stored15
    private val stored16: kotlin.ULong = element16
    val element16: kotlin.ULong get() = stored16
    private val stored17: kotlin.ULong = element17
    val element17: kotlin.ULong get() = stored17
    private val stored18: kotlin.ULong = element18
    val element18: kotlin.ULong get() = stored18
    fun copyValue(): SerdeJsonLargePowersArray5 = SerdeJsonLargePowersArray5(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18)
}
// Rust fixed array with 38 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray6(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong, element10: kotlin.ULong, element11: kotlin.ULong, element12: kotlin.ULong, element13: kotlin.ULong, element14: kotlin.ULong, element15: kotlin.ULong, element16: kotlin.ULong, element17: kotlin.ULong, element18: kotlin.ULong, element19: kotlin.ULong, element20: kotlin.ULong, element21: kotlin.ULong, element22: kotlin.ULong, element23: kotlin.ULong, element24: kotlin.ULong, element25: kotlin.ULong, element26: kotlin.ULong, element27: kotlin.ULong, element28: kotlin.ULong, element29: kotlin.ULong, element30: kotlin.ULong, element31: kotlin.ULong, element32: kotlin.ULong, element33: kotlin.ULong, element34: kotlin.ULong, element35: kotlin.ULong, element36: kotlin.ULong, element37: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    private val stored3: kotlin.ULong = element3
    val element3: kotlin.ULong get() = stored3
    private val stored4: kotlin.ULong = element4
    val element4: kotlin.ULong get() = stored4
    private val stored5: kotlin.ULong = element5
    val element5: kotlin.ULong get() = stored5
    private val stored6: kotlin.ULong = element6
    val element6: kotlin.ULong get() = stored6
    private val stored7: kotlin.ULong = element7
    val element7: kotlin.ULong get() = stored7
    private val stored8: kotlin.ULong = element8
    val element8: kotlin.ULong get() = stored8
    private val stored9: kotlin.ULong = element9
    val element9: kotlin.ULong get() = stored9
    private val stored10: kotlin.ULong = element10
    val element10: kotlin.ULong get() = stored10
    private val stored11: kotlin.ULong = element11
    val element11: kotlin.ULong get() = stored11
    private val stored12: kotlin.ULong = element12
    val element12: kotlin.ULong get() = stored12
    private val stored13: kotlin.ULong = element13
    val element13: kotlin.ULong get() = stored13
    private val stored14: kotlin.ULong = element14
    val element14: kotlin.ULong get() = stored14
    private val stored15: kotlin.ULong = element15
    val element15: kotlin.ULong get() = stored15
    private val stored16: kotlin.ULong = element16
    val element16: kotlin.ULong get() = stored16
    private val stored17: kotlin.ULong = element17
    val element17: kotlin.ULong get() = stored17
    private val stored18: kotlin.ULong = element18
    val element18: kotlin.ULong get() = stored18
    private val stored19: kotlin.ULong = element19
    val element19: kotlin.ULong get() = stored19
    private val stored20: kotlin.ULong = element20
    val element20: kotlin.ULong get() = stored20
    private val stored21: kotlin.ULong = element21
    val element21: kotlin.ULong get() = stored21
    private val stored22: kotlin.ULong = element22
    val element22: kotlin.ULong get() = stored22
    private val stored23: kotlin.ULong = element23
    val element23: kotlin.ULong get() = stored23
    private val stored24: kotlin.ULong = element24
    val element24: kotlin.ULong get() = stored24
    private val stored25: kotlin.ULong = element25
    val element25: kotlin.ULong get() = stored25
    private val stored26: kotlin.ULong = element26
    val element26: kotlin.ULong get() = stored26
    private val stored27: kotlin.ULong = element27
    val element27: kotlin.ULong get() = stored27
    private val stored28: kotlin.ULong = element28
    val element28: kotlin.ULong get() = stored28
    private val stored29: kotlin.ULong = element29
    val element29: kotlin.ULong get() = stored29
    private val stored30: kotlin.ULong = element30
    val element30: kotlin.ULong get() = stored30
    private val stored31: kotlin.ULong = element31
    val element31: kotlin.ULong get() = stored31
    private val stored32: kotlin.ULong = element32
    val element32: kotlin.ULong get() = stored32
    private val stored33: kotlin.ULong = element33
    val element33: kotlin.ULong get() = stored33
    private val stored34: kotlin.ULong = element34
    val element34: kotlin.ULong get() = stored34
    private val stored35: kotlin.ULong = element35
    val element35: kotlin.ULong get() = stored35
    private val stored36: kotlin.ULong = element36
    val element36: kotlin.ULong get() = stored36
    private val stored37: kotlin.ULong = element37
    val element37: kotlin.ULong get() = stored37
    fun copyValue(): SerdeJsonLargePowersArray6 = SerdeJsonLargePowersArray6(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37)
}
// Rust fixed array with 75 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray7(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong, element10: kotlin.ULong, element11: kotlin.ULong, element12: kotlin.ULong, element13: kotlin.ULong, element14: kotlin.ULong, element15: kotlin.ULong, element16: kotlin.ULong, element17: kotlin.ULong, element18: kotlin.ULong, element19: kotlin.ULong, element20: kotlin.ULong, element21: kotlin.ULong, element22: kotlin.ULong, element23: kotlin.ULong, element24: kotlin.ULong, element25: kotlin.ULong, element26: kotlin.ULong, element27: kotlin.ULong, element28: kotlin.ULong, element29: kotlin.ULong, element30: kotlin.ULong, element31: kotlin.ULong, element32: kotlin.ULong, element33: kotlin.ULong, element34: kotlin.ULong, element35: kotlin.ULong, element36: kotlin.ULong, element37: kotlin.ULong, element38: kotlin.ULong, element39: kotlin.ULong, element40: kotlin.ULong, element41: kotlin.ULong, element42: kotlin.ULong, element43: kotlin.ULong, element44: kotlin.ULong, element45: kotlin.ULong, element46: kotlin.ULong, element47: kotlin.ULong, element48: kotlin.ULong, element49: kotlin.ULong, element50: kotlin.ULong, element51: kotlin.ULong, element52: kotlin.ULong, element53: kotlin.ULong, element54: kotlin.ULong, element55: kotlin.ULong, element56: kotlin.ULong, element57: kotlin.ULong, element58: kotlin.ULong, element59: kotlin.ULong, element60: kotlin.ULong, element61: kotlin.ULong, element62: kotlin.ULong, element63: kotlin.ULong, element64: kotlin.ULong, element65: kotlin.ULong, element66: kotlin.ULong, element67: kotlin.ULong, element68: kotlin.ULong, element69: kotlin.ULong, element70: kotlin.ULong, element71: kotlin.ULong, element72: kotlin.ULong, element73: kotlin.ULong, element74: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    private val stored3: kotlin.ULong = element3
    val element3: kotlin.ULong get() = stored3
    private val stored4: kotlin.ULong = element4
    val element4: kotlin.ULong get() = stored4
    private val stored5: kotlin.ULong = element5
    val element5: kotlin.ULong get() = stored5
    private val stored6: kotlin.ULong = element6
    val element6: kotlin.ULong get() = stored6
    private val stored7: kotlin.ULong = element7
    val element7: kotlin.ULong get() = stored7
    private val stored8: kotlin.ULong = element8
    val element8: kotlin.ULong get() = stored8
    private val stored9: kotlin.ULong = element9
    val element9: kotlin.ULong get() = stored9
    private val stored10: kotlin.ULong = element10
    val element10: kotlin.ULong get() = stored10
    private val stored11: kotlin.ULong = element11
    val element11: kotlin.ULong get() = stored11
    private val stored12: kotlin.ULong = element12
    val element12: kotlin.ULong get() = stored12
    private val stored13: kotlin.ULong = element13
    val element13: kotlin.ULong get() = stored13
    private val stored14: kotlin.ULong = element14
    val element14: kotlin.ULong get() = stored14
    private val stored15: kotlin.ULong = element15
    val element15: kotlin.ULong get() = stored15
    private val stored16: kotlin.ULong = element16
    val element16: kotlin.ULong get() = stored16
    private val stored17: kotlin.ULong = element17
    val element17: kotlin.ULong get() = stored17
    private val stored18: kotlin.ULong = element18
    val element18: kotlin.ULong get() = stored18
    private val stored19: kotlin.ULong = element19
    val element19: kotlin.ULong get() = stored19
    private val stored20: kotlin.ULong = element20
    val element20: kotlin.ULong get() = stored20
    private val stored21: kotlin.ULong = element21
    val element21: kotlin.ULong get() = stored21
    private val stored22: kotlin.ULong = element22
    val element22: kotlin.ULong get() = stored22
    private val stored23: kotlin.ULong = element23
    val element23: kotlin.ULong get() = stored23
    private val stored24: kotlin.ULong = element24
    val element24: kotlin.ULong get() = stored24
    private val stored25: kotlin.ULong = element25
    val element25: kotlin.ULong get() = stored25
    private val stored26: kotlin.ULong = element26
    val element26: kotlin.ULong get() = stored26
    private val stored27: kotlin.ULong = element27
    val element27: kotlin.ULong get() = stored27
    private val stored28: kotlin.ULong = element28
    val element28: kotlin.ULong get() = stored28
    private val stored29: kotlin.ULong = element29
    val element29: kotlin.ULong get() = stored29
    private val stored30: kotlin.ULong = element30
    val element30: kotlin.ULong get() = stored30
    private val stored31: kotlin.ULong = element31
    val element31: kotlin.ULong get() = stored31
    private val stored32: kotlin.ULong = element32
    val element32: kotlin.ULong get() = stored32
    private val stored33: kotlin.ULong = element33
    val element33: kotlin.ULong get() = stored33
    private val stored34: kotlin.ULong = element34
    val element34: kotlin.ULong get() = stored34
    private val stored35: kotlin.ULong = element35
    val element35: kotlin.ULong get() = stored35
    private val stored36: kotlin.ULong = element36
    val element36: kotlin.ULong get() = stored36
    private val stored37: kotlin.ULong = element37
    val element37: kotlin.ULong get() = stored37
    private val stored38: kotlin.ULong = element38
    val element38: kotlin.ULong get() = stored38
    private val stored39: kotlin.ULong = element39
    val element39: kotlin.ULong get() = stored39
    private val stored40: kotlin.ULong = element40
    val element40: kotlin.ULong get() = stored40
    private val stored41: kotlin.ULong = element41
    val element41: kotlin.ULong get() = stored41
    private val stored42: kotlin.ULong = element42
    val element42: kotlin.ULong get() = stored42
    private val stored43: kotlin.ULong = element43
    val element43: kotlin.ULong get() = stored43
    private val stored44: kotlin.ULong = element44
    val element44: kotlin.ULong get() = stored44
    private val stored45: kotlin.ULong = element45
    val element45: kotlin.ULong get() = stored45
    private val stored46: kotlin.ULong = element46
    val element46: kotlin.ULong get() = stored46
    private val stored47: kotlin.ULong = element47
    val element47: kotlin.ULong get() = stored47
    private val stored48: kotlin.ULong = element48
    val element48: kotlin.ULong get() = stored48
    private val stored49: kotlin.ULong = element49
    val element49: kotlin.ULong get() = stored49
    private val stored50: kotlin.ULong = element50
    val element50: kotlin.ULong get() = stored50
    private val stored51: kotlin.ULong = element51
    val element51: kotlin.ULong get() = stored51
    private val stored52: kotlin.ULong = element52
    val element52: kotlin.ULong get() = stored52
    private val stored53: kotlin.ULong = element53
    val element53: kotlin.ULong get() = stored53
    private val stored54: kotlin.ULong = element54
    val element54: kotlin.ULong get() = stored54
    private val stored55: kotlin.ULong = element55
    val element55: kotlin.ULong get() = stored55
    private val stored56: kotlin.ULong = element56
    val element56: kotlin.ULong get() = stored56
    private val stored57: kotlin.ULong = element57
    val element57: kotlin.ULong get() = stored57
    private val stored58: kotlin.ULong = element58
    val element58: kotlin.ULong get() = stored58
    private val stored59: kotlin.ULong = element59
    val element59: kotlin.ULong get() = stored59
    private val stored60: kotlin.ULong = element60
    val element60: kotlin.ULong get() = stored60
    private val stored61: kotlin.ULong = element61
    val element61: kotlin.ULong get() = stored61
    private val stored62: kotlin.ULong = element62
    val element62: kotlin.ULong get() = stored62
    private val stored63: kotlin.ULong = element63
    val element63: kotlin.ULong get() = stored63
    private val stored64: kotlin.ULong = element64
    val element64: kotlin.ULong get() = stored64
    private val stored65: kotlin.ULong = element65
    val element65: kotlin.ULong get() = stored65
    private val stored66: kotlin.ULong = element66
    val element66: kotlin.ULong get() = stored66
    private val stored67: kotlin.ULong = element67
    val element67: kotlin.ULong get() = stored67
    private val stored68: kotlin.ULong = element68
    val element68: kotlin.ULong get() = stored68
    private val stored69: kotlin.ULong = element69
    val element69: kotlin.ULong get() = stored69
    private val stored70: kotlin.ULong = element70
    val element70: kotlin.ULong get() = stored70
    private val stored71: kotlin.ULong = element71
    val element71: kotlin.ULong get() = stored71
    private val stored72: kotlin.ULong = element72
    val element72: kotlin.ULong get() = stored72
    private val stored73: kotlin.ULong = element73
    val element73: kotlin.ULong get() = stored73
    private val stored74: kotlin.ULong = element74
    val element74: kotlin.ULong get() = stored74
    fun copyValue(): SerdeJsonLargePowersArray7 = SerdeJsonLargePowersArray7(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37, stored38, stored39, stored40, stored41, stored42, stored43, stored44, stored45, stored46, stored47, stored48, stored49, stored50, stored51, stored52, stored53, stored54, stored55, stored56, stored57, stored58, stored59, stored60, stored61, stored62, stored63, stored64, stored65, stored66, stored67, stored68, stored69, stored70, stored71, stored72, stored73, stored74)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37, stored38, stored39, stored40, stored41, stored42, stored43, stored44, stored45, stored46, stored47, stored48, stored49, stored50, stored51, stored52, stored53, stored54, stored55, stored56, stored57, stored58, stored59, stored60, stored61, stored62, stored63, stored64, stored65, stored66, stored67, stored68, stored69, stored70, stored71, stored72, stored73, stored74)
}
// Rust fixed array with 149 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray8(elements: kotlin.collections.List<kotlin.ULong>) {
    init { require(elements.size == 149) }
    private val stored: kotlin.collections.List<kotlin.ULong> = elements.map { it }
    fun copyValue(): SerdeJsonLargePowersArray8 = SerdeJsonLargePowersArray8(stored)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = stored.map { it }
}
// Rust fixed array with 298 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray9(elements: kotlin.collections.List<kotlin.ULong>) {
    init { require(elements.size == 298) }
    private val stored: kotlin.collections.List<kotlin.ULong> = elements.map { it }
    fun copyValue(): SerdeJsonLargePowersArray9 = SerdeJsonLargePowersArray9(stored)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = stored.map { it }
}
// Rust fixed array with 14 Copy elements; length is part of the type.
class SerdeJsonLargePowersArray10(element0: kotlin.collections.List<kotlin.ULong>, element1: kotlin.collections.List<kotlin.ULong>, element2: kotlin.collections.List<kotlin.ULong>, element3: kotlin.collections.List<kotlin.ULong>, element4: kotlin.collections.List<kotlin.ULong>, element5: kotlin.collections.List<kotlin.ULong>, element6: kotlin.collections.List<kotlin.ULong>, element7: kotlin.collections.List<kotlin.ULong>, element8: kotlin.collections.List<kotlin.ULong>, element9: kotlin.collections.List<kotlin.ULong>, element10: kotlin.collections.List<kotlin.ULong>, element11: kotlin.collections.List<kotlin.ULong>, element12: kotlin.collections.List<kotlin.ULong>, element13: kotlin.collections.List<kotlin.ULong>) {
    private val stored0: kotlin.collections.List<kotlin.ULong> = element0
    val element0: kotlin.collections.List<kotlin.ULong> get() = stored0
    private val stored1: kotlin.collections.List<kotlin.ULong> = element1
    val element1: kotlin.collections.List<kotlin.ULong> get() = stored1
    private val stored2: kotlin.collections.List<kotlin.ULong> = element2
    val element2: kotlin.collections.List<kotlin.ULong> get() = stored2
    private val stored3: kotlin.collections.List<kotlin.ULong> = element3
    val element3: kotlin.collections.List<kotlin.ULong> get() = stored3
    private val stored4: kotlin.collections.List<kotlin.ULong> = element4
    val element4: kotlin.collections.List<kotlin.ULong> get() = stored4
    private val stored5: kotlin.collections.List<kotlin.ULong> = element5
    val element5: kotlin.collections.List<kotlin.ULong> get() = stored5
    private val stored6: kotlin.collections.List<kotlin.ULong> = element6
    val element6: kotlin.collections.List<kotlin.ULong> get() = stored6
    private val stored7: kotlin.collections.List<kotlin.ULong> = element7
    val element7: kotlin.collections.List<kotlin.ULong> get() = stored7
    private val stored8: kotlin.collections.List<kotlin.ULong> = element8
    val element8: kotlin.collections.List<kotlin.ULong> get() = stored8
    private val stored9: kotlin.collections.List<kotlin.ULong> = element9
    val element9: kotlin.collections.List<kotlin.ULong> get() = stored9
    private val stored10: kotlin.collections.List<kotlin.ULong> = element10
    val element10: kotlin.collections.List<kotlin.ULong> get() = stored10
    private val stored11: kotlin.collections.List<kotlin.ULong> = element11
    val element11: kotlin.collections.List<kotlin.ULong> get() = stored11
    private val stored12: kotlin.collections.List<kotlin.ULong> = element12
    val element12: kotlin.collections.List<kotlin.ULong> get() = stored12
    private val stored13: kotlin.collections.List<kotlin.ULong> = element13
    val element13: kotlin.collections.List<kotlin.ULong> get() = stored13
    fun copyValue(): SerdeJsonLargePowersArray10 = SerdeJsonLargePowersArray10(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13)
    fun asSlice(): kotlin.collections.List<kotlin.collections.List<kotlin.ULong>> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13)
}
