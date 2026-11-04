// 아이디어 뽑기 — 홈 + 아이디어 기록 목업 (백엔드 없음, 상태는 메모리에만 저장)
// 구성: 데이터/모델 → 테마 팔레트 → 전역 저장소 → 앱/드로어 → 홈 화면 → 기록 화면
import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';

void main() {
  ErrorWidget.builder = (FlutterErrorDetails details) {
    if (details.exceptionAsString().contains('overflowed')) {
      return const SizedBox.shrink();
    }
    return ErrorWidget(details.exception);
  };
  runApp(const IdeaApp());
}

/* ───────────── 데이터 / 모델 ───────────── */

const Map<String, List<String>> kData = {
  '동물': ['수달', '올빼미', '문어', '고슴도치', '펭귄', '카멜레온', '너구리', '해파리', '알파카', '두더지'],
  '직업': ['소방관', '사서', '우주비행사', '바리스타', '수의사', '가이드', '정원사', '작곡가', '사진작가', '요리사'],
  '장소': ['도서관', '지하철역', '등대', '놀이공원', '옥상정원', '시장', '수족관', '기차역', '전망대', '박물관'],
  '감정': ['설렘', '심심함', '뿌듯함', '어색함', '그리움', '느긋함', '두근거림', '후련함', '통쾌함', '든든함'],
  '사물': ['우산', '손전등', '엽서', '자판기', '나침반', '열쇠', '풍선', '돋보기', '지도', '망원경'],
  '음식': ['떡볶이', '김밥', '붕어빵', '샌드위치', '냉면', '호떡', '수프', '컵라면', '비빔밥', '토스트'],
  '색깔': ['남색', '살구색', '연두', '보라', '청록', '분홍', '주황', '회색', '하늘색', '갈색'],
  '스포츠': ['수영', '볼링', '배드민턴', '클라이밍', '탁구', '양궁', '스키', '요가', '농구', '테니스'],
  '악기': ['기타', '트럼펫', '피아노', '바이올린', '드럼', '플루트', '하프', '오카리나', '첼로', '색소폰'],
  '탈것': ['기차', '자전거', '열기구', '케이블카', '트램', '스쿠터', '범선', '버스', '비행기', '킥보드'],
};
final List<String> kCats = kData.keys.toList();
const int kMinCount = 1, kMaxCount = 9;

enum Tier { rare, epic, unique, legend, mystic }

const Map<Tier, String> kTierName = {
  Tier.rare: '레어',
  Tier.epic: '에픽',
  Tier.unique: '유니크',
  Tier.legend: '레전드리',
  Tier.mystic: '미스틱',
};
int rank(Tier t) => t.index + 1;
bool glows(Tier t) => rank(t) >= 3; // 빛 효과는 유니크부터

Tier randTier(Random r) {
  final x = r.nextDouble();
  if (x < .52) return Tier.rare;
  if (x < .80) return Tier.epic;
  if (x < .94) return Tier.unique;
  if (x < .98) return Tier.legend;
  return Tier.mystic;
}

Tier bestOf(Iterable<Tier> ts) => ts.reduce((a, b) => rank(b) > rank(a) ? b : a);

class W {
  String word;
  Tier tier;
  bool locked;
  bool rolling;
  W(this.word, this.tier, {this.locked = false, this.rolling = false});
  W copy() => W(word, tier, locked: locked);
}

class WT {
  final String word;
  final Tier tier;
  const WT(this.word, this.tier);
}

class Idea {
  final List<WT> words;
  final Tier grade;
  final String text;
  final int tIdx;
  const Idea(this.words, this.grade, this.text, this.tIdx);
}

class SavedIdea {
  final int id;
  final String cat;
  final DateTime at;
  final Idea idea;
  SavedIdea(this.id, this.cat, this.at, this.idea);
}

class Tpl {
  final int min;
  final String Function(List<String>) f;
  const Tpl(this.min, this.f);
}

String _q(List<String> w) => w.map((x) => '“$x”').join(', ');
String _intro(List<String> w) =>
    '“${w.join(' · ')}, ${w.length > 1 ? '${w.length}개 단어가 만나는' : '한 단어로 시작하는'} 서비스”';

final List<Tpl> kTemplates = [
  Tpl(1, (w) => '${_q(w)} 키워드로 만드는 3분짜리 미니 게임. 한 판이 끝나면 키워드가 하나씩 바뀐다.'),
  Tpl(1, (w) => '${w.join(' × ')} 카드를 모으는 컬렉션 앱. 하루 한 장씩 새 카드가 열린다.'),
  Tpl(2, (w) => '${_q(w)}의 공통점을 찾는 퀴즈 앱. 친구와 점수를 겨룬다.'),
  Tpl(1, (w) => '${_q(w)} 키워드로 4컷 웹툰을 만드는 앱.'),
  Tpl(1, (w) => '${w.join('·')} 도감 서비스. 서로 닮은 점과 다른 점을 한 화면에서 비교한다.'),
];

/// 등급이 높을수록 아이디어가 구체적이 된다. 가장 높은 등급 단어가 핵심 키워드로 맨 앞에 온다.
String extraText(Tier g, List<String> w) {
  switch (g) {
    case Tier.rare:
      return '';
    case Tier.epic:
      return ' 핵심 키워드는 “${w[0]}”. 이 단어를 중심 규칙으로 삼아 비슷한 앱과 다른 점을 만든다.';
    case Tier.unique:
      return ' 핵심 키워드는 “${w[0]}”. 이 단어를 중심 규칙으로 삼고, 결과를 친구에게 공유하는 기능까지 넣어 출시 첫 주 목표를 잡는다.';
    case Tier.legend:
      return ' 핵심 키워드는 “${w[0]}”. 중심 규칙, 공유 기능, 시즌 이벤트까지 갖춘 완성형 기획이다. 한 줄 소개는 ${_intro(w)}.';
    case Tier.mystic:
      return ' 핵심 키워드는 “${w[0]}”. 중심 규칙, 공유 기능, 시즌 이벤트에 더해 처음 보는 사람도 바로 따라 쓰는 튜토리얼까지 갖춘 출시 직전 기획이다. 한 줄 소개는 ${_intro(w)}. 거의 나오지 않는 희귀한 조합이다.';
  }
}

Idea makeIdea(List<WT> ws, {int? forced, int prev = -1, Random? rnd}) {
  final r = rnd ?? Random();
  final ordered = [...ws]..sort((a, b) => rank(b.tier) - rank(a.tier));
  final w = ordered.map((s) => s.word).toList();
  final grade = bestOf(ws.map((s) => s.tier));
  final ok = [for (var k = 0; k < kTemplates.length; k++) if (kTemplates[k].min <= w.length) k];
  final others = ok.where((k) => k != prev).toList();
  final i = forced != null && ok.contains(forced) ? forced : (others.isNotEmpty ? others[r.nextInt(others.length)] : ok.first);
  return Idea(ordered, grade, kTemplates[i].f(w) + extraText(grade, w), i);
}

String whenLabel(DateTime at) {
  final d = DateTime.now().difference(at);
  if (d.inMinutes < 1) return '방금';
  if (d.inMinutes < 60) return '${d.inMinutes}분 전';
  if (d.inHours < 24) return '${d.inHours}시간 전';
  if (d.inDays < 7) return '${d.inDays}일 전';
  return '${d.inDays ~/ 7}주 전';
}

/* ───────────── 테마 팔레트 (라이트 / 다크) ───────────── */

class Pal {
  final Color bg, card, ink, muted, line, accent, accentInk, accentSoft, danger, dangerSoft;
  final Color potTop, potBottom, potBd, potMuted, rowBg, rowBd, rowLocked, rowIdle, onIcon;
  const Pal({
    required this.bg, required this.card, required this.ink, required this.muted, required this.line,
    required this.accent, required this.accentInk, required this.accentSoft, required this.danger, required this.dangerSoft,
    required this.potTop, required this.potBottom, required this.potBd, required this.potMuted,
    required this.rowBg, required this.rowBd, required this.rowLocked, required this.rowIdle, required this.onIcon,
  });

  static const light = Pal(
    bg: Color(0xFFF4F6FC), card: Color(0xFFFFFFFF), ink: Color(0xFF141A33), muted: Color(0xFF5A6483), line: Color(0xFFD3D9E8),
    accent: Color(0xFF9A6700), accentInk: Color(0xFFFFFFFF), accentSoft: Color(0xFFFFF0C7), danger: Color(0xFFB7322A), dangerSoft: Color(0xFFFBE6E3),
    potTop: Color(0xFFFFFFFF), potBottom: Color(0xFFEDF0F9), potBd: Color(0xFFCBD2E6), potMuted: Color(0xFF6A7391),
    rowBg: Color(0x0A141A33), rowBd: Color(0x1A141A33), rowLocked: Color(0x17141A33), rowIdle: Color(0xFF8A93B0), onIcon: Color(0xFFFFFFFF),
  );
  static const dark = Pal(
    bg: Color(0xFF0E1326), card: Color(0xFF1A2240), ink: Color(0xFFEEF1FF), muted: Color(0xFF8F98B8), line: Color(0xFF323C5E),
    accent: Color(0xFFFFC933), accentInk: Color(0xFF2B1D00), accentSoft: Color(0xFF3A3318), danger: Color(0xFFFF8A7E), dangerSoft: Color(0xFF3B2124),
    potTop: Color(0xFF1C2440), potBottom: Color(0xFF0F1428), potBd: Color(0xFF3B4670), potMuted: Color(0xFF8F98B8),
    rowBg: Color(0x0DFFFFFF), rowBd: Color(0x1AFFFFFF), rowLocked: Color(0x1AFFFFFF), rowIdle: Color(0xFF6D7699), onIcon: Color(0xFF10142A),
  );

  static Pal of(BuildContext c) => Theme.of(c).brightness == Brightness.dark ? dark : light;
}

Color tierColor(BuildContext c, Tier t) {
  final dark = Theme.of(c).brightness == Brightness.dark;
  const l = [Color(0xFF1B7FCB), Color(0xFF7A3FD6), Color(0xFFB07800), Color(0xFF14914D), Color(0xFFD12B45)];
  const d = [Color(0xFF5BC0FF), Color(0xFFB57CFF), Color(0xFFFFC933), Color(0xFF3DDC84), Color(0xFFFF5A6E)];
  return (dark ? d : l)[t.index];
}

const kGold = LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0xFFFFD65C), Color(0xFFE8A11C)]);
const kGoldInk = Color(0xFF2B1D00);

/* ───────────── 전역 저장소 (메모리) ───────────── */

class Store extends ChangeNotifier {
  ThemeMode mode = ThemeMode.system;
  final List<SavedIdea> saved = [];

  void setMode(ThemeMode m) { mode = m; notifyListeners(); }
  void add(String cat, Idea idea) { saved.insert(0, SavedIdea(DateTime.now().microsecondsSinceEpoch, cat, DateTime.now(), idea)); notifyListeners(); }
  void remove(int id) { saved.removeWhere((s) => s.id == id); notifyListeners(); }
}

final Store store = Store();

/* ───────────── 앱 / 드로어 ───────────── */

ThemeData _theme(Brightness b) {
  final p = b == Brightness.dark ? Pal.dark : Pal.light;
  return ThemeData(
    useMaterial3: true,
    brightness: b,
    scaffoldBackgroundColor: p.bg,
    colorScheme: ColorScheme.fromSeed(seedColor: p.accent, brightness: b).copyWith(surface: p.card, primary: p.accent, onPrimary: p.accentInk),
    appBarTheme: AppBarTheme(
      backgroundColor: p.bg, foregroundColor: p.ink, elevation: 0, scrolledUnderElevation: 0, centerTitle: true,
      titleTextStyle: TextStyle(color: p.ink, fontSize: 20, fontWeight: FontWeight.w700),
    ),
    drawerTheme: DrawerThemeData(backgroundColor: p.card),
    snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
  );
}

class IdeaApp extends StatelessWidget {
  const IdeaApp({super.key});
  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: store,
        builder: (context, _) => MaterialApp(
          title: '아이디어 뽑기',
          debugShowCheckedModeBanner: false,
          themeMode: store.mode,
          theme: _theme(Brightness.light),
          darkTheme: _theme(Brightness.dark),
          home: const HomePage(),
        ),
      );
}

enum Dest { home, log }

class AppDrawer extends StatelessWidget {
  final Dest current;
  const AppDrawer(this.current, {super.key});

  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Drawer(
      child: SafeArea(
        child: ListenableBuilder(
          listenable: store,
          builder: (context, _) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Padding(padding: const EdgeInsets.fromLTRB(20, 20, 20, 12), child: Text('아이디어 뽑기', style: TextStyle(color: p.ink, fontSize: 20, fontWeight: FontWeight.w700))),
            ListTile(
              selected: current == Dest.home, selectedColor: p.accent, selectedTileColor: p.accentSoft,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              title: const Text('홈'),
              onTap: () {
                Navigator.pop(context); // 드로어 닫기
                if (current != Dest.home) Navigator.pop(context); // 기록 → 홈
              },
            ),
            ListTile(
              selected: current == Dest.log, selectedColor: p.accent, selectedTileColor: p.accentSoft,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              title: const Text('아이디어 로그'),
              trailing: Badge(label: Text('${store.saved.length}'), isLabelVisible: store.saved.isNotEmpty, backgroundColor: p.accent, textColor: p.accentInk),
              onTap: () {
                Navigator.pop(context);
                if (current != Dest.log) Navigator.push(context, MaterialPageRoute(builder: (_) => const LogPage()));
              },
            ),
            const Spacer(),
            SwitchListTile(
              title: const Text('다크 모드'),
              value: dark,
              onChanged: (v) => store.setMode(v ? ThemeMode.dark : ThemeMode.light),
            ),
            ListTile(
              title: Text('로그아웃', style: TextStyle(color: p.muted)),
              onTap: () {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('목업이라 로그인 화면은 아직 없어요.')));
              },
            ),
          ]),
        ),
      ),
    );
  }
}

AppBar buildBar(BuildContext context, String title, VoidCallback onBack) => AppBar(
      automaticallyImplyLeading: false,
      leading: IconButton(icon: const Icon(Icons.chevron_left, size: 30), tooltip: '뒤로가기', onPressed: onBack),
      title: Text(title),
      actions: [
        Builder(builder: (c) => IconButton(icon: const Icon(Icons.menu, size: 26), tooltip: '메뉴 열기', onPressed: () => Scaffold.of(c).openEndDrawer())),
      ],
    );

class GoldButton extends StatelessWidget {
  final String label;
  final IconData? icon;
  final VoidCallback? onPressed;
  const GoldButton({super.key, required this.label, this.icon, this.onPressed});
  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    final on = onPressed != null;
    return Material(
      color: Colors.transparent,
      child: Ink(
        height: 48,
        decoration: BoxDecoration(gradient: on ? kGold : null, color: on ? null : p.line, borderRadius: BorderRadius.circular(14)),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onPressed,
          child: Center(
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              if (icon != null) ...[Icon(icon, size: 22, color: on ? kGoldInk : p.muted), const SizedBox(width: 8)],
              Text(label, style: TextStyle(color: on ? kGoldInk : p.muted, fontWeight: FontWeight.w700, fontSize: 16)),
            ]),
          ),
        ),
      ),
    );
  }
}

/* 아이디어 카드: 홈 미리보기와 기록 목록이 같은 모양을 쓴다 */
class IdeaCard extends StatelessWidget {
  final Idea idea;
  final String tag;
  final bool clamp;
  const IdeaCard({super.key, required this.idea, required this.tag, this.clamp = false});
  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    final tc = tierColor(context, idea.grade);
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: p.card, borderRadius: BorderRadius.circular(18), border: Border.all(color: tc),
        boxShadow: glows(idea.grade) ? [BoxShadow(color: tc.withValues(alpha: .35), blurRadius: 18, spreadRadius: -6)] : null,
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Flexible(child: Text(tag, style: TextStyle(color: p.muted, fontSize: 12))),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
            decoration: BoxDecoration(border: Border.all(color: tc), borderRadius: BorderRadius.circular(99)),
            child: Text('${kTierName[idea.grade]}${clamp ? '' : ' 아이디어'}', style: TextStyle(color: tc, fontSize: 12, fontWeight: FontWeight.w600)),
          ),
        ]),
        const SizedBox(height: 8),
        Text.rich(TextSpan(children: [
          for (var i = 0; i < idea.words.length; i++) ...[
            if (i > 0) TextSpan(text: ' × ', style: TextStyle(color: p.ink)),
            TextSpan(text: idea.words[i].word, style: TextStyle(color: tierColor(context, idea.words[i].tier))),
          ],
        ]), style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w700, height: 1.3)),
        const SizedBox(height: 6),
        Text(idea.text, maxLines: clamp ? 2 : null, overflow: clamp ? TextOverflow.ellipsis : null, style: TextStyle(color: clamp ? p.muted : p.ink, fontSize: clamp ? 14 : 15, height: 1.5)),
      ]),
    );
  }
}

/* ───────────── 홈 화면 ───────────── */

class _Pending {
  final List<W> before;
  final Idea? idea;
  final bool saved;
  final int tIdx;
  _Pending(this.before, this.idea, this.saved, this.tIdx);
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _ctrl = TextEditingController();
  final _focus = FocusNode();
  final _rnd = Random();
  String? cat;
  int count = 3;
  List<W> words = [];
  List<String> flicker = [];
  Idea? idea;
  int tIdx = -1;
  bool loading = false, busy = false, saved = false;
  _Pending? pending;
  Timer? _rollTimer, _genTimer;
  final List<Timer> _timers = [];

  bool get frozen => loading || busy || pending != null;

  @override
  void initState() {
    super.initState();
    _focus.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _stopRoll();
    _genTimer?.cancel();
    _ctrl.dispose();
    _focus.dispose();
    super.dispose();
  }

  Idea _build() {
    final it = makeIdea(words.map((w) => WT(w.word, w.tier)).toList(), prev: tIdx, rnd: _rnd);
    tIdx = it.tIdx;
    saved = false;
    return it;
  }

  void _stopRoll() {
    _rollTimer?.cancel();
    _rollTimer = null;
    for (final t in _timers) { t.cancel(); }
    _timers.clear();
  }

  void _snack(String m) => ScaffoldMessenger.of(context)..hideCurrentSnackBar()..showSnackBar(SnackBar(content: Text(m), duration: const Duration(milliseconds: 1600)));

  List<W> _draw(String c, List<W> cur) {
    final used = <String>{for (final w in cur) if (w.locked) w.word};
    return List.generate(count, (i) {
      if (i < cur.length && cur[i].locked) return cur[i];
      final pool = kData[c]!.where((x) => !used.contains(x) && (i >= cur.length || x != cur[i].word)).toList();
      final w = pool[_rnd.nextInt(pool.length)];
      used.add(w);
      return W(w, randTier(_rnd));
    });
  }

  /// 잠재 연출: 고정되지 않은 줄이 섞이다가 위에서부터 하나씩 확정된다.
  void _roll(List<int> idx) {
    _stopRoll();
    if (idx.isEmpty) return;
    final pool = kData[cat]!;
    setState(() {
      busy = true;
      flicker = List.filled(words.length, '');
      for (var i = 0; i < words.length; i++) { words[i].rolling = idx.contains(i); }
    });
    _rollTimer = Timer.periodic(const Duration(milliseconds: 70), (_) {
      if (!mounted) return;
      setState(() { for (final i in idx) { if (words[i].rolling) flicker[i] = pool[_rnd.nextInt(pool.length)]; } });
    });
    final step = min(450, 1350 ~/ max(1, idx.length - 1));
    for (var k = 0; k < idx.length; k++) {
      final i = idx[k];
      _timers.add(Timer(Duration(milliseconds: 800 + k * step), () { if (mounted) setState(() => words[i].rolling = false); }));
    }
    _timers.add(Timer(Duration(milliseconds: 800 + (idx.length - 1) * step + 700), () {
      if (!mounted) return;
      _stopRoll();
      setState(() { busy = false; for (final w in words) { w.rolling = false; } });
    }));
  }

  void _selectCat(String c) {
    _stopRoll();
    setState(() {
      cat = c; idea = null; pending = null; saved = false; tIdx = -1;
      words = [];
      _ctrl.text = c;
    });
    _focus.unfocus();
  }

  void _onQuery(String v) {
    // 글자를 고치면 선택이 풀린다. 목록에 없는 값은 쓸 수 없다.
    setState(() { if (cat != null && v != cat) { _stopRoll(); cat = null; words = []; idea = null; pending = null; busy = false; saved = false; } });
  }

  void _setCount(int n) {
    if (n == count || frozen) return;
    _stopRoll();
    setState(() {
      count = n;
      words = [];
      idea = null;
      pending = null;
      saved = false;
      tIdx = -1;
    });
  }

  void _drawWords() {
    if (cat == null || frozen) return;
    setState(() {
      words = _draw(cat!, []);
      idea = null;
      pending = null;
      saved = false;
      tIdx = -1;
    });
  }

  void _reroll() {
    final idx = [for (var i = 0; i < words.length; i++) if (!words[i].locked) i];
    if (idx.isEmpty) return;
    setState(() {
      pending ??= _Pending(words.map((w) => w.copy()).toList(), idea, saved, tIdx);
      words = _draw(cat!, words);
      idea = null; saved = false;
    });
    _roll(idx);
  }

  void _keepPrev() {
    final p = pending!;
    setState(() { words = p.before.map((w) => w.copy()).toList(); idea = p.idea; saved = p.saved; tIdx = p.tIdx; pending = null; });
    _snack('이전 결과를 유지했어요.');
  }

  void _takeNext() { setState(() => pending = null); _snack('이후 결과를 선택했어요.'); }

  void _generate() {
    if (frozen || words.isEmpty) return; // 로딩/뽑는 중/선택 대기 중 재요청 차단
    setState(() => loading = true);
    _genTimer?.cancel();
    _genTimer = Timer(const Duration(milliseconds: 1100), () {
      if (!mounted) return;
      setState(() { loading = false; idea = _build(); });
    });
  }

  void _save() {
    if (idea == null || saved || loading || busy) return;
    store.add(cat!, idea!);
    setState(() => saved = true);
    _snack('저장했어요. 아이디어 기록에서 볼 수 있어요.');
  }

  /* ── 위젯 ── */

  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    return Scaffold(
      appBar: buildBar(context, '아이디어 뽑기', () => Navigator.maybePop(context)),
      endDrawer: const AppDrawer(Dest.home),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
              children: [
                _catRow(p),
                ..._suggestions(p),
                const SizedBox(height: 14),
                words.isEmpty ? (cat == null ? _emptyBox(p) : _drawWordsBox(p)) : _pot(p),
                const SizedBox(height: 14),
                if (words.isNotEmpty && idea == null) ...[
                  GoldButton(
                    label: loading ? '만드는 중…' : (pending != null && !busy ? '이전/이후를 먼저 선택해주세요' : '아이디어 만들기'),
                    onPressed: frozen ? null : _generate,
                  ),
                  if (loading)
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: LinearProgressIndicator(minHeight: 4, color: p.accent, backgroundColor: p.accentSoft),
                      ),
                    ),
                  const SizedBox(height: 10),
                ],
                if (words.isNotEmpty) _ideaSection(p),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _catRow(Pal p) {
    return Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Expanded(
        child: Container(
          height: 48,
          padding: const EdgeInsets.only(left: 12, right: 4),
          decoration: BoxDecoration(color: p.card, borderRadius: BorderRadius.circular(14), border: Border.all(color: _focus.hasFocus ? p.accent : p.line)),
          child: Row(children: [
            Icon(Icons.search, size: 20, color: p.muted),
            const SizedBox(width: 8),
            Expanded(
              child: TextField(
                controller: _ctrl, focusNode: _focus, enabled: !frozen,
                onChanged: _onQuery, onTapOutside: (_) {},
                onSubmitted: (_) { final s = _sug; if (s.isNotEmpty) _selectCat(s.first); },
                decoration: InputDecoration.collapsed(hintText: '카테고리 검색', hintStyle: TextStyle(color: p.muted)),
                style: TextStyle(color: p.ink, fontSize: 16),
              ),
            ),
            if (_ctrl.text.isNotEmpty && !frozen)
              IconButton(
                icon: Icon(Icons.close, size: 18, color: p.muted), tooltip: '카테고리 지우기',
                onPressed: () { _stopRoll(); _ctrl.clear(); setState(() { cat = null; words = []; idea = null; pending = null; busy = false; saved = false; }); _focus.requestFocus(); },
              ),
          ]),
        ),
      ),
      const SizedBox(width: 10),
      _countBox(p),
    ]);
  }

  Widget _countBox(Pal p) {
    return PopupMenuButton<int>(
      enabled: !frozen,
      tooltip: '단어 개수 선택',
      onSelected: _setCount,
      color: p.card,
      elevation: 6,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      constraints: const BoxConstraints.tightFor(width: 160),
      padding: EdgeInsets.zero,
      itemBuilder: (ctx) => [
        PopupMenuItem<int>(
          enabled: false,
          padding: EdgeInsets.zero,
          height: 160,
          child: Container(
            width: 160,
            height: 160,
            padding: const EdgeInsets.all(8),
            alignment: Alignment.center,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                for (var r = 0; r < 3; r++) ...[
                  if (r > 0) const SizedBox(height: 6),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      for (var c = 0; c < 3; c++) ...[
                        if (c > 0) const SizedBox(width: 6),
                        Builder(builder: (itemCtx) {
                          final n = r * 3 + c + 1;
                          final isSel = n == count;
                          return InkWell(
                            borderRadius: BorderRadius.circular(10),
                            onTap: () {
                              Navigator.pop(ctx, n);
                              _setCount(n);
                            },
                            child: Container(
                              width: 44,
                              height: 44,
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                color: isSel ? p.accent : p.rowBg,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(color: isSel ? p.accent : p.line),
                              ),
                              child: Text(
                                '$n',
                                style: TextStyle(
                                  color: isSel ? p.accentInk : p.ink,
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                          );
                        }),
                      ],
                    ],
                  ),
                ],
              ],
            ),
          ),
        ),
      ],
      child: Container(
        width: 48,
        height: 48,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: p.card,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: p.line),
        ),
        child: Text.rich(
          TextSpan(text: '$count', children: [
            TextSpan(text: '개', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w500, color: p.muted)),
          ]),
          style: TextStyle(color: p.ink, fontWeight: FontWeight.w700, fontSize: 16),
        ),
      ),
    );
  }

  List<String> get _sug {
    final q = _ctrl.text.trim();
    return q.isEmpty ? kCats : kCats.where((c) => c.contains(q)).toList();
  }

  List<Widget> _suggestions(Pal p) {
    final q = _ctrl.text.trim();
    if (_focus.hasFocus && !frozen) {
      final s = _sug;
      return [
        const SizedBox(height: 6),
        Container(
          constraints: const BoxConstraints(maxHeight: 216),
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(color: p.card, borderRadius: BorderRadius.circular(14), border: Border.all(color: p.line)),
          child: s.isEmpty
              ? Padding(padding: const EdgeInsets.all(12), child: Text('일치하는 카테고리가 없어요', style: TextStyle(color: p.muted)))
              : ListView(shrinkWrap: true, children: [
                  for (final c in s)
                    ListTile(
                      dense: true, title: Text(c, style: TextStyle(fontWeight: cat == c ? FontWeight.w700 : FontWeight.w400, color: cat == c ? p.accent : p.ink)),
                      trailing: cat == c ? Text('선택됨', style: TextStyle(color: p.muted, fontSize: 12)) : null,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      onTap: () => _selectCat(c),
                    ),
                ]),
        ),
      ];
    }
    if (q.isNotEmpty && cat == null) {
      final none = _sug.isEmpty;
      return [Padding(padding: const EdgeInsets.fromLTRB(2, 6, 2, 0), child: Text(none ? '‘$q’는 없는 카테고리예요. 목록에 있는 카테고리만 쓸 수 있어요.' : '목록에서 카테고리를 눌러 선택해주세요.', style: TextStyle(color: none ? p.danger : p.muted, fontSize: 13)))];
    }
    return const [];
  }

  Widget _emptyBox(Pal p) => Container(
        padding: const EdgeInsets.symmetric(vertical: 22, horizontal: 16),
        decoration: BoxDecoration(borderRadius: BorderRadius.circular(16), border: Border.all(color: p.line, width: 1.5)),
        child: Column(children: [
          Text('카테고리를 선택해주세요', style: TextStyle(color: p.ink, fontWeight: FontWeight.w600)),
          const SizedBox(height: 2),
          Text('위 검색창에서 카테고리를 고르고 단어를 뽑아보세요.', style: TextStyle(color: p.muted), textAlign: TextAlign.center),
        ]),
      );

  Widget _drawWordsBox(Pal p) => Container(
        padding: const EdgeInsets.symmetric(vertical: 22, horizontal: 16),
        decoration: BoxDecoration(color: p.card, borderRadius: BorderRadius.circular(16), border: Border.all(color: p.line, width: 1.5)),
        child: Column(children: [
          Text('‘$cat’ 카테고리가 선택되었어요', style: TextStyle(color: p.ink, fontWeight: FontWeight.w700, fontSize: 16)),
          const SizedBox(height: 4),
          Text('단어 뽑기 버튼을 누르면 추천 단어 $count개가 생성돼요.', style: TextStyle(color: p.muted, fontSize: 14), textAlign: TextAlign.center),
          const SizedBox(height: 16),
          SizedBox(
            width: 200,
            child: GoldButton(
              label: '단어 뽑기',
              icon: Icons.casino_outlined,
              onPressed: _drawWords,
            ),
          ),
        ]),
      );

  Widget _pot(Pal p) {
    final best = bestOf(words.map((w) => w.tier));
    final tc = tierColor(context, best);
    final pend = pending;
    final noneFree = words.every((w) => w.locked);
    var pill = '${kTierName[best]} 등급';
    if (busy) {
      pill = '뽑는 중…';
    } else if (pend != null) {
      final b = rank(bestOf(pend.before.map((w) => w.tier))), a = rank(best);
      pill = '${kTierName[best]} ${a > b ? '▲' : a < b ? '▼' : '–'}';
    }
    final pillColor = busy ? p.potMuted : tc;
    final dense = words.length >= 6;
    final int n = words.length;
    final double dividers = n > 1 ? (n - 1) * 1.0 : 0.0;
    // m: 확정 상태(첫 뽑기 / 선택 이후)의 단어 한 줄 높이 → 단어 영역 H = n*m (+구분선)
    final double m = n == 1 ? 56.0 : (dense ? 44.0 : 48.0);
    final double singleBoxH = n > 0 ? n * m + dividers : 0.0;

    // 리롤 상태: 고정 버튼 높이 (다시뽑기의 60% = 28.8px)
    const double btnH = 28.8;
    // n*m과 고정 버튼 크기(btnH)를 바탕으로 각 상황(n=1~9)에서 여유 공간(spacing)과 비교 단어 타일 크기 정밀 계산:
    // available = singleBoxH - btnH
    // 각 n별 최적의 spacing:
    // n=1: 4.2 (타일 높이 23.0px 확보)
    // n=2: 8.2 (타일 높이 29.5px)
    // n=3: 10.2 (타일 높이 35.0px)
    // n=4: 12.2 (타일 높이 37.75px)
    // n=5,6: 14.2 (타일 높이 39.4px / 36.83px)
    // n=7: 15.2 (타일 높이 37.71px)
    // n=8,9: 16.2 (타일 높이 38.375px / 39.0px)
    // 따라서 compBoxH + spacing + btnH == singleBoxH 가 100% 일치
    final double spacing = n <= 1
        ? 4.2
        : (n == 2
            ? 8.2
            : (n == 3
                ? 10.2
                : (n == 4
                    ? 12.2
                    : (n <= 6 ? 14.2 : (n == 7 ? 15.2 : 16.2)))));
    final double compBoxH = (singleBoxH - btnH - spacing).clamp(0.0, double.infinity);
    final double compRowH = n > 0 ? (compBoxH - dividers) / n : 0.0;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: p.card,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: busy ? p.potBd : tc, width: 1.5),
        boxShadow: !busy && glows(best) ? [BoxShadow(color: tc.withValues(alpha: .35), blurRadius: 24, spreadRadius: -8)] : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 1. 장소 (space between) 등급
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                cat ?? '',
                style: TextStyle(color: p.ink, fontSize: 18, fontWeight: FontWeight.w700),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                decoration: BoxDecoration(
                  border: Border.all(color: pillColor),
                  borderRadius: BorderRadius.circular(99),
                ),
                child: Text(pill, style: TextStyle(color: pillColor, fontSize: 12, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // 2. 단어 영역: 확정 상태든 리롤 상태든 항상 같은 높이(singleBoxH)를 점유
          if (pend == null)
            _singleWordsBox(p, boxHeight: singleBoxH, rowHeight: m, dense: dense)
          else
            SizedBox(
              height: singleBoxH,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Expanded(
                          child: _comparisonWordsBox(
                            p,
                            [for (var i = 0; i < words.length; i++) i < pend.before.length ? pend.before[i] : words[i]],
                            isNew: false,
                            dense: dense,
                            rowHeight: compRowH,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: _comparisonWordsBox(
                            p,
                            words,
                            isNew: true,
                            dense: dense,
                            rowHeight: compRowH,
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: spacing),
                  _selButtons(
                    p,
                    btnH,
                    prevColor: tierColor(context, bestOf(pend.before.map((w) => w.tier))),
                    nextColor: busy ? p.accent : tc,
                  ),
                ],
              ),
            ),
          const SizedBox(height: 10),

          // 4. 다시 뽑기 버튼 (위치 고정)
          GoldButton(
            label: pend != null ? '다시 뽑기' : (words.any((w) => w.locked) ? '다시 뽑기' : '전체 다시 뽑기'),
            icon: Icons.casino_outlined,
            onPressed: busy || loading || noneFree ? null : _reroll,
          ),
        ],
      ),
    );
  }

  // 유지 / 선택 버튼 (각자 최고등급 색상 적용, 뽑기 중 비활성화)
  Widget _selButtons(Pal p, double h, {required Color prevColor, required Color nextColor}) {
    return Row(
      children: [
        Expanded(
          child: SizedBox(
            height: h,
            child: OutlinedButton(
              onPressed: busy || loading ? null : _keepPrev,
              style: OutlinedButton.styleFrom(
                foregroundColor: busy ? p.muted : prevColor,
                disabledForegroundColor: p.muted.withValues(alpha: 0.5),
                side: BorderSide(color: busy ? p.line.withValues(alpha: 0.5) : prevColor, width: 1.3),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                padding: EdgeInsets.zero,
              ),
              child: const Text('유지', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13)),
            ),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: SizedBox(
            height: h,
            child: OutlinedButton(
              onPressed: busy || loading ? null : _takeNext,
              style: OutlinedButton.styleFrom(
                foregroundColor: busy ? p.muted : nextColor,
                disabledForegroundColor: p.muted.withValues(alpha: 0.5),
                side: BorderSide(color: busy ? p.line.withValues(alpha: 0.5) : nextColor, width: 1.5),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                padding: EdgeInsets.zero,
              ),
              child: const Text('선택', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13)),
            ),
          ),
        ),
      ],
    );
  }

  // 평소 1개의 박스 형태 단어 목록
  Widget _singleWordsBox(
    Pal p, {
    required double boxHeight,
    required double rowHeight,
    required bool dense,
  }) {
    return Container(
      height: boxHeight,
      decoration: BoxDecoration(
        color: p.rowBg,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: p.line),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(13),
        child: Column(
          children: [
            for (var i = 0; i < words.length; i++) ...[
              if (i > 0) Divider(height: 1, thickness: 1, color: p.line),
              Expanded(
                child: _wordTile(p, words[i], i, dense: dense),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _wordTile(Pal p, W w, int i, {required bool dense}) {
    final c = w.rolling ? p.rowIdle : tierColor(context, w.tier);
    final text = w.rolling ? (i < flicker.length && flicker[i].isNotEmpty ? flicker[i] : '…') : w.word;
    final shadow = !w.rolling && glows(w.tier) ? [Shadow(color: c.withValues(alpha: .65), blurRadius: 10)] : null;
    return Material(
      color: w.locked ? p.rowLocked : Colors.transparent,
      child: InkWell(
        onTap: frozen ? null : () => setState(() => w.locked = !w.locked),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: Center(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Icon(w.locked ? Icons.lock : Icons.diamond, size: 20, color: c),
                const SizedBox(width: 10),
                Expanded(
                  child: _vc(Text(
                    text,
                    style: TextStyle(color: c, fontSize: 17, fontWeight: FontWeight.w700, shadows: shadow, height: 1.2, leadingDistribution: TextLeadingDistribution.even),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ), 17),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(border: Border.all(color: c), borderRadius: BorderRadius.circular(99)),
                  child: _vc(Text(w.rolling ? '???' : kTierName[w.tier]!, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, height: 1.2, leadingDistribution: TextLeadingDistribution.even).copyWith(color: c)), 11),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 한글 폰트는 ascent/descent가 비대칭이라 글자가 살짝 아래로 처져 보인다 → 글자 크기 비례로 위로 보정해 시각적 중앙 정렬
  Widget _vc(Widget child, double fontSize) => Transform.translate(offset: Offset(0, -fontSize * 0.07), child: child);

  // 리롤 시 2개의 박스(이전/이후) 중 하나: 결과창과 동일한 행 높이, 위아래 불필요한 패딩 없이 각 행 중앙 정렬
  Widget _comparisonWordsBox(
    Pal p,
    List<W> wordList, {
    required bool isNew,
    required bool dense,
    required double rowHeight,
  }) {
    final best = bestOf(wordList.map((w) => w.tier));
    final tc = tierColor(context, best);
    final borderColor = (isNew && busy) ? p.potBd : tc;

    return Container(
      decoration: BoxDecoration(
        color: p.rowBg,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: borderColor, width: 1.5),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(13),
        child: Column(
          children: [
            for (var i = 0; i < wordList.length; i++) ...[
              if (i > 0) Divider(height: 1, thickness: 1, color: p.line),
              Expanded(
                child: _compWordTile(p, wordList[i], i, isNew: isNew, dense: dense, rowHeight: rowHeight),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _compWordTile(Pal p, W w, int i, {required bool isNew, required bool dense, required double rowHeight}) {
    final isRolling = isNew && w.rolling;
    final c = isRolling ? p.rowIdle : tierColor(context, w.tier);
    final text = isRolling ? (i < flicker.length && flicker[i].isNotEmpty ? flicker[i] : '…') : w.word;
    final shadow = !isRolling && glows(w.tier) ? [Shadow(color: c.withValues(alpha: .65), blurRadius: 8)] : null;
    final double iconSize = rowHeight < 25 ? 12 : (dense ? 13 : 15);
    final double fontSize = rowHeight < 25 ? 11.5 : (dense ? 12.5 : 13.5);
    final double badgeFont = rowHeight < 25 ? 7.5 : (dense ? 8.5 : 9.0);
    final EdgeInsets badgePad = rowHeight < 25
        ? const EdgeInsets.symmetric(horizontal: 4, vertical: 0.5)
        : const EdgeInsets.symmetric(horizontal: 5, vertical: 1);

    return Container(
      color: w.locked ? p.rowLocked : Colors.transparent,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Center(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Icon(w.locked ? Icons.lock : Icons.diamond, size: iconSize, color: c),
            const SizedBox(width: 6),
            Expanded(
              child: _vc(Text(
                text,
                style: TextStyle(color: c, fontSize: fontSize, fontWeight: FontWeight.w700, shadows: shadow, height: 1.2, leadingDistribution: TextLeadingDistribution.even),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ), fontSize),
            ),
            const SizedBox(width: 4),
            Container(
              padding: badgePad,
              decoration: BoxDecoration(border: Border.all(color: c, width: 0.8), borderRadius: BorderRadius.circular(99)),
              child: _vc(Text(
                isRolling ? '???' : kTierName[w.tier]!,
                style: TextStyle(color: c, fontSize: badgeFont, fontWeight: FontWeight.w600, height: 1.2, leadingDistribution: TextLeadingDistribution.even),
              ), badgeFont),
            ),
          ],
        ),
      ),
    );
  }

  Widget _ideaSection(Pal p) {
    if (idea == null) {
      final msg = loading ? '단어를 조합하고 있어요…' : busy ? '단어를 뽑는 중이에요' : pending != null ? '이전과 이후 중 하나를 선택하면 아이디어를 만들 수 있어요.' : words.isNotEmpty ? '위 버튼을 누르면 단어로 아이디어를 만들어요.' : '단어를 뽑으면 여기에 아이디어가 나와요.';
      return Container(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 14), alignment: Alignment.center,
        decoration: BoxDecoration(borderRadius: BorderRadius.circular(18), border: Border.all(color: p.line, style: BorderStyle.solid)),
        child: Text(msg, textAlign: TextAlign.center, style: TextStyle(color: p.muted)),
      );
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      IdeaCard(idea: idea!, tag: '만들어진 아이디어 · $cat'),
      const SizedBox(height: 12),
      Row(children: [
        Expanded(
          flex: 10,
          child: SizedBox(
            height: 48,
            child: OutlinedButton(
              onPressed: (frozen && !loading) || busy ? null : _generate,
              style: OutlinedButton.styleFrom(
                foregroundColor: p.ink,
                side: BorderSide(color: p.line),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              child: Text(loading ? '만드는 중…' : '다시 만들기', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(flex: 14, child: SizedBox(height: 48, child: _SaveBtn(saved: saved, onTap: _save))),
      ]),
      if (loading)
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(minHeight: 4, color: p.accent, backgroundColor: p.accentSoft),
          ),
        ),
    ]);
  }
}

class _SaveBtn extends StatelessWidget {
  final bool saved;
  final VoidCallback onTap;
  const _SaveBtn({required this.saved, required this.onTap});
  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    return Material(
      color: Colors.transparent,
      child: Ink(
        decoration: BoxDecoration(gradient: saved ? null : kGold, color: saved ? p.accentSoft : null, borderRadius: BorderRadius.circular(12)),
        child: InkWell(
          borderRadius: BorderRadius.circular(12), onTap: saved ? null : onTap,
          child: Center(child: Text(saved ? '저장됨' : '아이디어 저장', style: TextStyle(color: saved ? p.accent : kGoldInk, fontWeight: FontWeight.w700, fontSize: 15))),
        ),
      ),
    );
  }
}

/* ───────────── 아이디어 기록 화면 ───────────── */

const bool kSimulateLogError = false; // true로 바꾸면 오류 상태를 볼 수 있다 (백엔드 연결 시 실제 오류로 대체)

enum LogStatus { loading, ready, error }

class LogPage extends StatefulWidget {
  const LogPage({super.key});
  @override
  State<LogPage> createState() => _LogPageState();
}

class _LogPageState extends State<LogPage> {
  LogStatus status = LogStatus.loading;
  bool edit = false;
  Timer? _t;

  @override
  void initState() { super.initState(); _load(); }

  @override
  void dispose() { _t?.cancel(); super.dispose(); }

  void _load() {
    setState(() => status = LogStatus.loading);
    _t?.cancel();
    _t = Timer(const Duration(milliseconds: 700), () { if (mounted) setState(() => status = kSimulateLogError ? LogStatus.error : LogStatus.ready); });
  }

  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    return Scaffold(
      appBar: buildBar(context, '아이디어 기록', () => Navigator.maybePop(context)),
      endDrawer: const AppDrawer(Dest.log),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: ListenableBuilder(listenable: store, builder: (context, _) => _content(p)),
          ),
        ),
      ),
    );
  }

  Widget _content(Pal p) {
    if (status == LogStatus.loading) {
      return ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 24), children: [
        for (var i = 0; i < 4; i++) Container(height: 116, margin: const EdgeInsets.only(bottom: 10), decoration: BoxDecoration(color: p.card, borderRadius: BorderRadius.circular(18), border: Border.all(color: p.line))),
      ]);
    }
    if (status == LogStatus.error) {
      return ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 24), children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(color: p.dangerSoft, borderRadius: BorderRadius.circular(14)),
          child: Row(children: [
            Expanded(child: Text('기록을 불러오지 못했어요.', style: TextStyle(color: p.danger))),
            OutlinedButton(onPressed: _load, style: OutlinedButton.styleFrom(foregroundColor: p.danger, side: BorderSide(color: p.danger)), child: const Text('다시 시도')),
          ]),
        ),
      ]);
    }
    final items = store.saved;
    if (items.isEmpty) {
      return ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 24), children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 16),
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(16), border: Border.all(color: p.line, width: 1.5)),
          child: Column(children: [
            Text('아이디어를 생성해보세요', style: TextStyle(color: p.ink, fontWeight: FontWeight.w600)),
            const SizedBox(height: 2),
            Text('홈에서 단어를 뽑고 저장하면 여기에 쌓여요.', style: TextStyle(color: p.muted)),
            const SizedBox(height: 14),
            SizedBox(width: 220, child: GoldButton(label: '아이디어 만들러 가기', onPressed: () => Navigator.maybePop(context))),
          ]),
        ),
      ]);
    }
    return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
      Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
        Text('저장한 아이디어 ${items.length}개', style: TextStyle(color: p.muted, fontSize: 14)),
        TextButton(onPressed: () => setState(() => edit = !edit), child: Text(edit ? '완료' : '편집', style: TextStyle(color: p.accent, fontWeight: FontWeight.w600))),
      ]),
      for (final s in items)
        Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            IdeaCard(idea: s.idea, tag: '${s.cat} · ${whenLabel(s.at)}', clamp: true),
            if (edit)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: OutlinedButton(
                  onPressed: () {
                    store.remove(s.id);
                    if (store.saved.isEmpty) setState(() => edit = false);
                  },
                  style: OutlinedButton.styleFrom(foregroundColor: p.danger, side: BorderSide(color: p.danger), minimumSize: const Size.fromHeight(40), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10))),
                  child: const Text('삭제'),
                ),
              ),
          ]),
        ),
    ]);
  }
}
