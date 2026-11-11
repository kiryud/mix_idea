// 아이디어 뽑기 — 플러터 프론트엔드 (스프링부트 백엔드 REST API 완전 연동 + 오프라인 목업 Fallback)
import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'models/models.dart';
import 'services/api_service.dart';
import 'services/storage_service.dart';
import 'screens/login_screen.dart';
import 'screens/feed_screen.dart';
import 'screens/guide_dialog.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  ErrorWidget.builder = (FlutterErrorDetails details) {
    if (details.exceptionAsString().contains('overflowed')) {
      return const SizedBox.shrink();
    }
    return ErrorWidget(details.exception);
  };

  await ApiService().init();
  await StorageService().init();

  runApp(const IdeaApp());
}

/* ───────────── 오프라인 Fallback 목업 데이터 ───────────── */

const Map<String, List<String>> kFallbackData = {
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

final List<String> kFallbackCats = kFallbackData.keys.toList();

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

String extraText(Tier g, List<String> w) {
  switch (g) {
    case Tier.rare:
      return '';
    case Tier.epic:
      return ' 핵심 키워드는 “${w.isNotEmpty ? w[0] : ''}”. 이 단어를 중심 규칙으로 삼아 비슷한 앱과 다른 점을 만든다.';
    case Tier.unique:
      return ' 핵심 키워드는 “${w.isNotEmpty ? w[0] : ''}”. 이 단어를 중심 규칙으로 삼고, 결과를 친구에게 공유하는 기능까지 넣어 출시 첫 주 목표를 잡는다.';
    case Tier.legend:
      return ' 핵심 키워드는 “${w.isNotEmpty ? w[0] : ''}”. 중심 규칙, 공유 기능, 시즌 이벤트까지 갖춘 완성형 기획이다. 한 줄 소개는 ${_intro(w)}.';
    case Tier.mystic:
      return ' 핵심 키워드는 “${w.isNotEmpty ? w[0] : ''}”. 중심 규칙, 공유 기능, 시즌 이벤트에 더해 처음 보는 사람도 바로 따라 쓰는 튜토리얼까지 갖춘 출시 직전 기획이다. 한 줄 소개는 ${_intro(w)}. 거의 나오지 않는 희귀한 조합이다.';
  }
}

IdeaItem makeOfflineIdea(List<WordItem> ws, String catName, {int prev = -1, Random? rnd}) {
  final r = rnd ?? Random();
  final ordered = [...ws]..sort((a, b) => rank(b.tier) - rank(a.tier));
  final w = ordered.map((s) => s.text).toList();
  final grade = bestOf(ws.map((s) => s.tier));
  final ok = [for (var k = 0; k < kTemplates.length; k++) if (kTemplates[k].min <= w.length) k];
  final others = ok.where((k) => k != prev).toList();
  final i = others.isNotEmpty ? others[r.nextInt(others.length)] : (ok.isNotEmpty ? ok.first : 0);
  final text = (ok.isNotEmpty ? kTemplates[i].f(w) : '아이디어') + extraText(grade, w);

  return IdeaItem(
    id: 'local_${DateTime.now().millisecondsSinceEpoch}',
    categoryId: 'local_cat',
    categoryName: catName,
    title: w.isNotEmpty ? '${w.join(" × ")} 기획' : '아이디어',
    content: text,
    words: ordered,
    grade: grade,
    source: 'mock',
    createdAt: DateTime.now(),
  );
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

/* ───────────── 전역 앱 상태 ───────────── */

class AppState extends ChangeNotifier {
  ThemeMode mode = ThemeMode.system;
  bool guestMode = false;

  void setMode(ThemeMode m) {
    mode = m;
    notifyListeners();
  }

  void setGuestMode(bool v) {
    guestMode = v;
    notifyListeners();
  }
}

final AppState appState = AppState();

/* ───────────── 루트 앱 위젯 ───────────── */

ThemeData _theme(Brightness b) {
  final p = b == Brightness.dark ? Pal.dark : Pal.light;
  return ThemeData(
    useMaterial3: true,
    brightness: b,
    scaffoldBackgroundColor: p.bg,
    colorScheme: ColorScheme.fromSeed(seedColor: p.accent, brightness: b).copyWith(
      surface: p.card,
      primary: p.accent,
      onPrimary: p.accentInk,
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: p.bg,
      foregroundColor: p.ink,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      titleTextStyle: TextStyle(color: p.ink, fontSize: 20, fontWeight: FontWeight.w700),
    ),
    drawerTheme: DrawerThemeData(backgroundColor: p.card),
    snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
  );
}

class IdeaApp extends StatelessWidget {
  const IdeaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: appState,
      builder: (context, _) {
        final api = ApiService();
        final showLogin = !api.isLoggedIn && !appState.guestMode;

        return MaterialApp(
          title: '아이디어 뽑기',
          debugShowCheckedModeBanner: false,
          themeMode: appState.mode,
          theme: _theme(Brightness.light),
          darkTheme: _theme(Brightness.dark),
          home: showLogin
              ? LoginScreen(
                  onLoginSuccess: () => appState.setGuestMode(false),
                  onGuestMode: () => appState.setGuestMode(true),
                )
              : const HomePage(),
        );
      },
    );
  }
}

/* ───────────── 앱 드로어 ───────────── */

enum Dest { home, feed, log }

class AppDrawer extends StatelessWidget {
  final Dest current;
  const AppDrawer(this.current, {super.key});

  void _showServerSettings(BuildContext context) {
    final api = ApiService();
    final ctrl = TextEditingController(text: api.baseUrl);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('스프링부트 서버 주소'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '백엔드 API URL을 설정합니다:\n(Desktop/Web: http://localhost:8080)\n(Android 에뮬레이터: http://10.0.2.2:8080)',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: ctrl,
              decoration: const InputDecoration(border: OutlineInputBorder()),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('취소')),
          FilledButton(
            onPressed: () async {
              await api.setBaseUrl(ctrl.text.trim());
              if (context.mounted) {
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('서버 주소: ${api.baseUrl}')),
                );
              }
            },
            child: const Text('저장'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    final dark = Theme.of(context).brightness == Brightness.dark;
    final api = ApiService();
    final session = api.session;
    final storage = StorageService();

    return Drawer(
      child: SafeArea(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 6),
            child: Row(
              children: [
                Icon(Icons.lightbulb, color: p.accent, size: 28),
                const SizedBox(width: 8),
                Text('아이디어 뽑기', style: TextStyle(color: p.ink, fontSize: 20, fontWeight: FontWeight.w700)),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
            child: Text(
              api.isLoggedIn
                  ? '👤 ${session?.name ?? session?.id ?? ""} 님 (${session?.id ?? ""})'
                  : '👤 게스트 모드 (오프라인)',
              style: TextStyle(color: p.muted, fontSize: 13, fontWeight: FontWeight.w600),
            ),
          ),
          const Divider(),

          ListTile(
            leading: const Icon(Icons.home_outlined),
            selected: current == Dest.home,
            selectedColor: p.accent,
            selectedTileColor: p.accentSoft,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            title: const Text('홈 (아이디어 뽑기)'),
            onTap: () {
              Navigator.pop(context);
              if (current != Dest.home) {
                Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const HomePage()));
              }
            },
          ),
          ListTile(
            leading: const Icon(Icons.dynamic_feed_outlined),
            selected: current == Dest.feed,
            selectedColor: p.accent,
            selectedTileColor: p.accentSoft,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            title: const Text('피드 (공유된 아이디어)'),
            onTap: () {
              Navigator.pop(context);
              if (current != Dest.feed) {
                Navigator.push(context, MaterialPageRoute(builder: (_) => const FeedScreen()));
              }
            },
          ),
          ListTile(
            leading: const Icon(Icons.bookmark_outline),
            selected: current == Dest.log,
            selectedColor: p.accent,
            selectedTileColor: p.accentSoft,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            title: const Text('아이디어 기록'),
            trailing: ListenableBuilder(
              listenable: storage,
              builder: (ctx, _) => Badge(
                label: Text('${storage.records.length}'),
                isLabelVisible: storage.records.isNotEmpty,
                backgroundColor: p.accent,
                textColor: p.accentInk,
              ),
            ),
            onTap: () {
              Navigator.pop(context);
              if (current != Dest.log) {
                Navigator.push(context, MaterialPageRoute(builder: (_) => const LogPage()));
              }
            },
          ),

          const Spacer(),
          const Divider(),

          ListTile(
            leading: const Icon(Icons.dns_outlined),
            title: const Text('서버 URL 설정'),
            subtitle: Text(api.baseUrl, style: const TextStyle(fontSize: 11), overflow: TextOverflow.ellipsis),
            onTap: () => _showServerSettings(context),
          ),
          SwitchListTile(
            secondary: const Icon(Icons.dark_mode_outlined),
            title: const Text('다크 모드'),
            value: dark,
            onChanged: (v) => appState.setMode(v ? ThemeMode.dark : ThemeMode.light),
          ),
          ListTile(
            leading: Icon(api.isLoggedIn ? Icons.logout : Icons.login, color: p.muted),
            title: Text(api.isLoggedIn ? '로그아웃' : '로그인하러 가기', style: TextStyle(color: p.muted)),
            onTap: () async {
              Navigator.pop(context);
              if (api.isLoggedIn) {
                await api.logout();
                appState.setGuestMode(false);
              } else {
                appState.setGuestMode(false);
              }
            },
          ),
        ]),
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

/* ───────────── 아이디어 카드 위젯 ───────────── */

class IdeaCard extends StatelessWidget {
  final IdeaItem idea;
  final String tag;
  final bool clamp;
  final VoidCallback? onGuide;
  final VoidCallback? onShare;
  final VoidCallback? onPost;

  const IdeaCard({
    super.key,
    required this.idea,
    required this.tag,
    this.clamp = false,
    this.onGuide,
    this.onShare,
    this.onPost,
  });

  @override
  Widget build(BuildContext context) {
    final p = Pal.of(context);
    final tc = tierColor(context, idea.grade);

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: p.card,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: tc),
        boxShadow: glows(idea.grade) ? [BoxShadow(color: tc.withValues(alpha: .35), blurRadius: 18, spreadRadius: -6)] : null,
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Flexible(
            child: Text(
              idea.title.isNotEmpty ? idea.title : tag,
              style: TextStyle(color: p.muted, fontSize: 13, fontWeight: FontWeight.bold),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (idea.source == 'mock')
                Container(
                  margin: const EdgeInsets.only(right: 6),
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(color: Colors.amber.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(4)),
                  child: const Text('Mock', style: TextStyle(color: Colors.amber, fontSize: 10, fontWeight: FontWeight.bold)),
                ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
                decoration: BoxDecoration(border: Border.all(color: tc), borderRadius: BorderRadius.circular(99)),
                child: Text('${kTierName[idea.grade]}${clamp ? '' : ' 아이디어'}', style: TextStyle(color: tc, fontSize: 12, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
        ]),
        const SizedBox(height: 8),

        // 단어 태그들
        if (idea.words.isNotEmpty)
          Text.rich(
            TextSpan(children: [
              for (var i = 0; i < idea.words.length; i++) ...[
                if (i > 0) TextSpan(text: ' × ', style: TextStyle(color: p.ink)),
                TextSpan(text: idea.words[i].text, style: TextStyle(color: tierColor(context, idea.words[i].tier))),
              ],
            ]),
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700, height: 1.3),
          ),
        const SizedBox(height: 6),

        Text(
          idea.content,
          maxLines: clamp ? 2 : null,
          overflow: clamp ? TextOverflow.ellipsis : null,
          style: TextStyle(color: clamp ? p.muted : p.ink, fontSize: clamp ? 14 : 15, height: 1.5),
        ),

        if (!clamp && (onGuide != null || onShare != null || onPost != null)) ...[
          const SizedBox(height: 12),
          const Divider(height: 1),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 6,
            children: [
              if (onGuide != null)
                OutlinedButton.icon(
                  icon: const Icon(Icons.auto_stories, size: 16),
                  label: Text('${idea.guideLabel} 보기'),
                  onPressed: onGuide,
                  style: OutlinedButton.styleFrom(
                    visualDensity: VisualDensity.compact,
                    foregroundColor: p.accent,
                    side: BorderSide(color: p.accent),
                  ),
                ),
              if (onShare != null)
                OutlinedButton.icon(
                  icon: const Icon(Icons.link, size: 16),
                  label: const Text('링크 복사'),
                  onPressed: onShare,
                  style: OutlinedButton.styleFrom(
                    visualDensity: VisualDensity.compact,
                    foregroundColor: p.ink,
                    side: BorderSide(color: p.line),
                  ),
                ),
              if (onPost != null)
                OutlinedButton.icon(
                  icon: const Icon(Icons.share, size: 16),
                  label: const Text('피드에 올리기'),
                  onPressed: onPost,
                  style: OutlinedButton.styleFrom(
                    visualDensity: VisualDensity.compact,
                    foregroundColor: p.ink,
                    side: BorderSide(color: p.line),
                  ),
                ),
            ],
          ),
        ],
      ]),
    );
  }
}

/* ───────────── 홈 화면 ───────────── */

class _Pending {
  final List<WordItem> before;
  final IdeaItem? idea;
  final bool saved;
  _Pending(this.before, this.idea, this.saved);
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final ApiService _api = ApiService();
  final StorageService _storage = StorageService();
  final _ctrl = TextEditingController();
  final _dirCtrl = TextEditingController();
  final _focus = FocusNode();
  final _rnd = Random();

  List<CategoryItem> serverCategories = [];
  CategoryItem? selectedCategory;

  int count = 3;
  List<WordItem> words = [];
  List<String> flicker = [];
  IdeaItem? idea;
  GuideItem? guide;

  bool loadingCategories = false;
  bool loading = false, busy = false, saved = false;
  _Pending? pending;
  Timer? _rollTimer;
  final List<Timer> _timers = [];

  bool get frozen => loading || busy || pending != null;

  @override
  void initState() {
    super.initState();
    _focus.addListener(() => setState(() {}));
    _fetchCategories();
  }

  @override
  void dispose() {
    _stopRoll();
    _ctrl.dispose();
    _dirCtrl.dispose();
    _focus.dispose();
    super.dispose();
  }

  Future<void> _fetchCategories() async {
    setState(() => loadingCategories = true);
    try {
      if (_api.isLoggedIn) {
        final list = await _api.getCategories();
        if (mounted) {
          setState(() {
            serverCategories = list;
            loadingCategories = false;
          });
          return;
        }
      }
    } catch (_) {}

    // Fallback: 오프라인 기본 카테고리
    if (mounted) {
      setState(() {
        serverCategories = kFallbackCats
            .map((c) => CategoryItem(
                  id: 'fallback_$c',
                  name: c,
                  wordCount: kFallbackData[c]?.length ?? 10,
                  guideType: c == '음식' ? 'food' : 'general',
                  guideLabel: c == '음식' ? '만드는 법' : '실행 계획',
                ))
            .toList();
        loadingCategories = false;
      });
    }
  }

  void _stopRoll() {
    _rollTimer?.cancel();
    _rollTimer = null;
    for (final t in _timers) {
      t.cancel();
    }
    _timers.clear();
  }

  void _snack(String m) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(m), duration: const Duration(milliseconds: 1800)));
  }

  // 1. 단어 뽑기 (온라인 백엔드 draw 또는 오프라인 fallback)
  Future<void> _drawWords() async {
    if (selectedCategory == null || frozen) return;
    final cat = selectedCategory!;
    final direction = _dirCtrl.text.trim();

    setState(() {
      busy = true;
      idea = null;
      guide = null;
      pending = null;
      saved = false;
    });

    try {
      if (_api.isLoggedIn && !cat.id.startsWith('fallback_')) {
        final result = await _api.drawWords(
          categoryId: cat.id,
          direction: direction,
        );
        if (mounted) {
          setState(() {
            words = result;
            busy = false;
          });
          return;
        }
      }
    } catch (e) {
      _snack('서버 단어 뽑기 실패, 오프라인으로 대체합니다: $e');
    }

    // Fallback 로컬 뽑기
    final pool = kFallbackData[cat.name] ?? ['단어1', '단어2', '단어3', '단어4', '단어5'];
    final picked = <String>[];
    while (picked.length < count && picked.length < pool.length) {
      final w = pool[_rnd.nextInt(pool.length)];
      if (!picked.contains(w)) picked.add(w);
    }
    setState(() {
      words = picked.map((t) => WordItem(id: 'local_${_rnd.nextInt(9999)}', text: t, tier: randTier(_rnd))).toList();
      busy = false;
    });
  }

  // 2. 다시 뽑기 (리롤 & 롤링 애니메이션)
  Future<void> _reroll() async {
    if (selectedCategory == null || frozen) return;
    final cat = selectedCategory!;
    final freeIdx = [for (var i = 0; i < words.length; i++) if (!words[i].locked) i];
    if (freeIdx.isEmpty) return;

    final prevWords = words.map((w) => w.copy()).toList();
    final lockedIds = words.where((w) => w.locked).map((w) => w.id).where((id) => id.isNotEmpty).toList();
    final previousIds = words.map((w) => w.id).where((id) => id.isNotEmpty).toList();
    final direction = _dirCtrl.text.trim();

    // 롤링 애니메이션 시작
    _stopRoll();
    setState(() {
      busy = true;
      pending ??= _Pending(prevWords, idea, saved);
      idea = null;
      guide = null;
      saved = false;
      flicker = List.filled(words.length, '');
      for (var i = 0; i < words.length; i++) {
        words[i].rolling = freeIdx.contains(i);
      }
    });

    final pool = kFallbackData[cat.name] ?? ['별', '빛', '바람', '구름', '꿈'];
    _rollTimer = Timer.periodic(const Duration(milliseconds: 70), (_) {
      if (!mounted) return;
      setState(() {
        for (final i in freeIdx) {
          if (words[i].rolling) flicker[i] = pool[_rnd.nextInt(pool.length)];
        }
      });
    });

    List<WordItem>? newWords;
    try {
      if (_api.isLoggedIn && !cat.id.startsWith('fallback_')) {
        newWords = await _api.drawWords(
          categoryId: cat.id,
          lockedWordIds: lockedIds,
          previousWordIds: previousIds,
          direction: direction,
        );
      }
    } catch (_) {}

    // Fallback if not server
    if (newWords == null || newWords.isEmpty) {
      final used = <String>{for (final w in words) if (w.locked) w.text};
      newWords = List.generate(words.length, (i) {
        if (words[i].locked) return words[i].copy();
        final avail = pool.where((x) => !used.contains(x)).toList();
        final w = avail.isNotEmpty ? avail[_rnd.nextInt(avail.length)] : pool[_rnd.nextInt(pool.length)];
        used.add(w);
        return WordItem(id: 'local_${_rnd.nextInt(9999)}', text: w, tier: randTier(_rnd));
      });
    }

    // 애니메이션 멈추고 새 단어 적용
    final step = min(450, 1350 ~/ max(1, freeIdx.length - 1));
    for (var k = 0; k < freeIdx.length; k++) {
      final i = freeIdx[k];
      _timers.add(Timer(Duration(milliseconds: 700 + k * step), () {
        if (mounted) setState(() => words[i].rolling = false);
      }));
    }
    _timers.add(Timer(Duration(milliseconds: 700 + (freeIdx.length - 1) * step + 500), () {
      if (!mounted) return;
      _stopRoll();
      setState(() {
        busy = false;
        words = newWords!;
      });
    }));
  }

  void _keepPrev() {
    final p = pending!;
    setState(() {
      words = p.before.map((w) => w.copy()).toList();
      idea = p.idea;
      saved = p.saved;
      pending = null;
    });
    _snack('이전 결과를 유지했어요.');
  }

  void _takeNext() {
    setState(() => pending = null);
    _snack('이후 결과를 선택했어요.');
  }

  // 3. 아이디어 생성 (스프링부트 POST /app/ideas 호출)
  Future<void> _generate() async {
    if (frozen || words.isEmpty || selectedCategory == null) return;
    final cat = selectedCategory!;
    final direction = _dirCtrl.text.trim();

    setState(() => loading = true);

    try {
      if (_api.isLoggedIn && !cat.id.startsWith('fallback_')) {
        // 단어 ID 3개
        final wordIds = words.map((w) => w.id).where((id) => id.isNotEmpty).toList();
        if (wordIds.length >= 3) {
          final resIdea = await _api.createIdea(
            categoryId: cat.id,
            categoryName: cat.name,
            wordIds: wordIds.take(3).toList(),
            direction: direction,
          );
          if (mounted) {
            setState(() {
              loading = false;
              idea = resIdea;
              guide = null;
              saved = _storage.isSaved(resIdea.id);
            });
            return;
          }
        }
      }
    } catch (e) {
      _snack('서버 생성 오류: $e (로컬 생성으로 대체합니다)');
    }

    // Fallback 로컬 생성
    await Future.delayed(const Duration(milliseconds: 800));
    if (!mounted) return;
    final fallbackIdea = makeOfflineIdea(words, cat.name, rnd: _rnd);
    setState(() {
      loading = false;
      idea = fallbackIdea;
      guide = null;
      saved = false;
    });
  }

  // 4. 다시 만들기 (recombine)
  Future<void> _recombine() async {
    if (idea == null || loading || busy) return;
    if (_api.isLoggedIn && !idea!.id.startsWith('local_')) {
      setState(() => loading = true);
      try {
        final recombined = await _api.recombineIdea(
          ideaId: idea!.id,
          categoryName: idea!.categoryName,
        );
        if (mounted) {
          setState(() {
            loading = false;
            idea = recombined;
            guide = null;
            saved = false;
          });
          return;
        }
      } catch (e) {
        _snack('재조합 실패: $e');
      }
    }
    _generate();
  }

  // 5. 후속 가이드 보기
  Future<void> _showGuide() async {
    if (idea == null) return;
    if (guide != null) {
      GuideDialog.show(context, guide!);
      return;
    }

    if (_api.isLoggedIn && !idea!.id.startsWith('local_')) {
      setState(() => loading = true);
      try {
        final g = await _api.getGuide(idea!.id);
        if (mounted) {
          setState(() {
            guide = g;
            loading = false;
          });
          GuideDialog.show(context, g);
          return;
        }
      } catch (e) {
        _snack('가이드 생성 실패: $e');
        setState(() => loading = false);
        return;
      }
    }

    // Fallback 모의 가이드
    final dummy = GuideItem(
      ideaId: idea!.id,
      kind: idea!.guideType,
      label: idea!.guideLabel,
      requirementsLabel: idea!.guideType == 'food' ? '필요한 재료' : '준비사항',
      feasible: true,
      summary: '${idea!.title}를 구현하기 위한 권장 진행 계획입니다.',
      requirements: [
        const GuideRequirement(name: '핵심 준비', detail: '기본 기획서 및 사용자 인터뷰 준비'),
        const GuideRequirement(name: '기술 스택', detail: 'Flutter 프론트엔드 + Spring Boot 백엔드'),
      ],
      steps: [
        const GuideStep(order: 1, title: '기획 구체화', description: '핵심 키워드를 중심으로 MVP 기능 정의'),
        const GuideStep(order: 2, title: '프로토타입 개발', description: '화면 흐름과 기본 UI 컴포넌트 구성'),
        const GuideStep(order: 3, title: '배포 및 피드백', description: '사용자 테스트를 거쳐 기능 개선'),
      ],
      source: 'mock',
    );
    if (!mounted) return;
    GuideDialog.show(context, dummy);
  }

  // 6. 링크 공유
  Future<void> _shareLink() async {
    if (idea == null) return;
    try {
      if (_api.isLoggedIn && !idea!.id.startsWith('local_')) {
        final url = await _api.createShareLink(idea!.id);
        await Clipboard.setData(ClipboardData(text: url));
        _snack('공유 링크가 클립보드에 복사되었어요: $url');
        return;
      }
    } catch (e) {
      _snack('링크 생성 실패: $e');
      return;
    }
    await Clipboard.setData(ClipboardData(text: '아이디어: ${idea!.title}\n${idea!.content}'));
    _snack('아이디어 텍스트가 클립보드에 복사되었어요.');
  }

  // 7. 피드에 올리기
  Future<void> _postToFeed() async {
    if (idea == null) return;
    if (!_api.isLoggedIn || idea!.id.startsWith('local_')) {
      _snack('피드 게시 기능은 스프링부트 로그인 후 사용 가능합니다.');
      return;
    }
    try {
      await _api.createPost(idea!.id);
      _snack('피드에 아이디어가 게시되었습니다! 피드 탭에서 확인하세요.');
    } catch (e) {
      _snack('피드 등록 실패: $e');
    }
  }

  // 8. 아이디어 저장 (로컬 보관)
  Future<void> _save() async {
    if (idea == null || saved) return;
    await _storage.save(idea!, guide: guide);
    setState(() => saved = true);
    _snack('저장했어요. 아이디어 기록에서 볼 수 있어요.');
  }

  void _selectCat(CategoryItem c) {
    _stopRoll();
    setState(() {
      selectedCategory = c;
      idea = null;
      guide = null;
      pending = null;
      saved = false;
      words = [];
      _ctrl.text = c.name;
    });
    _focus.unfocus();
  }

  void _showAddCategoryDialog() {
    final nameCtrl = TextEditingController();
    String guideType = 'general';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: const Text('새 카테고리 추가'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: nameCtrl,
                decoration: const InputDecoration(
                  labelText: '카테고리 이름',
                  hintText: '예: 우주 탐사, 반려동물',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 14),
              const Text('가이드 유형:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const SizedBox(height: 6),
              DropdownButtonFormField<String>(
                initialValue: guideType,
                decoration: const InputDecoration(border: OutlineInputBorder()),
                items: const [
                  DropdownMenuItem(value: 'general', child: Text('일반 (실행 계획)')),
                  DropdownMenuItem(value: 'food', child: Text('음식 (만드는 법)')),
                  DropdownMenuItem(value: 'software', child: Text('소프트웨어 (구현 가이드)')),
                  DropdownMenuItem(value: 'contest', child: Text('공모전 (기획서)')),
                ],
                onChanged: (v) {
                  if (v != null) setDialogState(() => guideType = v);
                },
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('취소')),
            FilledButton(
              onPressed: () async {
                final name = nameCtrl.text.trim();
                if (name.isEmpty) return;
                Navigator.pop(ctx);
                try {
                  final newCat = await _api.createCategory(name: name, guideType: guideType);
                  await _fetchCategories();
                  _selectCat(newCat);
                  _snack('‘$name’ 카테고리가 추가되었습니다.');
                } catch (e) {
                  _snack('카테고리 추가 실패: $e');
                }
              },
              child: const Text('추가'),
            ),
          ],
        ),
      ),
    );
  }

  List<CategoryItem> get _sug {
    final q = _ctrl.text.trim();
    return q.isEmpty
        ? serverCategories
        : serverCategories.where((c) => c.name.contains(q)).toList();
  }

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
                const SizedBox(height: 10),

                // 방향성 입력창 (선택)
                if (selectedCategory != null) ...[
                  TextField(
                    controller: _dirCtrl,
                    maxLength: 200,
                    enabled: !frozen,
                    decoration: InputDecoration(
                      labelText: '방향성 (선택사항, 200자 이내)',
                      hintText: '예: 간편하고 매콤한 캠핑 요리, 직장인을 위한 앱',
                      counterText: '',
                      prefixIcon: const Icon(Icons.compass_calibration_outlined, size: 20),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                    ),
                  ),
                  const SizedBox(height: 10),
                ],

                words.isEmpty
                    ? (selectedCategory == null ? _emptyBox(p) : _drawWordsBox(p))
                    : _pot(p),
                const SizedBox(height: 14),

                if (words.isNotEmpty && idea == null) ...[
                  GoldButton(
                    label: loading
                        ? '만드는 중…'
                        : (pending != null && !busy ? '이전/이후를 먼저 선택해주세요' : '아이디어 만들기'),
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
          decoration: BoxDecoration(
            color: p.card,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: _focus.hasFocus ? p.accent : p.line),
          ),
          child: Row(children: [
            Icon(Icons.search, size: 20, color: p.muted),
            const SizedBox(width: 8),
            Expanded(
              child: TextField(
                controller: _ctrl,
                focusNode: _focus,
                enabled: !frozen,
                onChanged: (v) {
                  setState(() {
                    if (selectedCategory != null && v != selectedCategory!.name) {
                      _stopRoll();
                      selectedCategory = null;
                      words = [];
                      idea = null;
                      guide = null;
                      pending = null;
                      busy = false;
                      saved = false;
                    }
                  });
                },
                decoration: InputDecoration.collapsed(
                  hintText: '카테고리 검색 또는 선택',
                  hintStyle: TextStyle(color: p.muted),
                ),
                style: TextStyle(color: p.ink, fontSize: 16),
              ),
            ),
            if (_ctrl.text.isNotEmpty && !frozen)
              IconButton(
                icon: Icon(Icons.close, size: 18, color: p.muted),
                tooltip: '카테고리 지우기',
                onPressed: () {
                  _stopRoll();
                  _ctrl.clear();
                  setState(() {
                    selectedCategory = null;
                    words = [];
                    idea = null;
                    guide = null;
                    pending = null;
                    busy = false;
                    saved = false;
                  });
                  _focus.requestFocus();
                },
              ),
          ]),
        ),
      ),
      const SizedBox(width: 8),
      IconButton.filledTonal(
        icon: const Icon(Icons.add),
        tooltip: '새 카테고리 추가',
        onPressed: frozen ? null : _showAddCategoryDialog,
      ),
    ]);
  }

  List<Widget> _suggestions(Pal p) {
    if (_focus.hasFocus && !frozen) {
      final s = _sug;
      return [
        const SizedBox(height: 6),
        Container(
          constraints: const BoxConstraints(maxHeight: 220),
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: p.card,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: p.line),
          ),
          child: s.isEmpty
              ? Padding(
                  padding: const EdgeInsets.all(12),
                  child: Text('일치하는 카테고리가 없어요', style: TextStyle(color: p.muted)),
                )
              : ListView.builder(
                  shrinkWrap: true,
                  itemCount: s.length,
                  itemBuilder: (ctx, i) {
                    final c = s[i];
                    final isSel = selectedCategory?.id == c.id;
                    return ListTile(
                      dense: true,
                      title: Text(
                        c.name,
                        style: TextStyle(
                          fontWeight: isSel ? FontWeight.w700 : FontWeight.w400,
                          color: isSel ? p.accent : p.ink,
                        ),
                      ),
                      subtitle: c.wordCount > 0 ? Text('${c.wordCount}개 단어', style: TextStyle(fontSize: 11, color: p.muted)) : null,
                      trailing: isSel ? Text('선택됨', style: TextStyle(color: p.muted, fontSize: 12)) : null,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      onTap: () => _selectCat(c),
                    );
                  },
                ),
        ),
      ];
    }
    return const [];
  }

  Widget _emptyBox(Pal p) => Container(
        padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 16),
        decoration: BoxDecoration(borderRadius: BorderRadius.circular(16), border: Border.all(color: p.line, width: 1.5)),
        child: Column(children: [
          Text('카테고리를 선택해주세요', style: TextStyle(color: p.ink, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text('위 검색창에서 카테고리를 고르고 단어를 뽑아보세요.', style: TextStyle(color: p.muted), textAlign: TextAlign.center),
        ]),
      );

  Widget _drawWordsBox(Pal p) => Container(
        padding: const EdgeInsets.symmetric(vertical: 22, horizontal: 16),
        decoration: BoxDecoration(color: p.card, borderRadius: BorderRadius.circular(16), border: Border.all(color: p.line, width: 1.5)),
        child: Column(children: [
          Text('‘${selectedCategory?.name}’ 카테고리가 선택되었어요', style: TextStyle(color: p.ink, fontWeight: FontWeight.w700, fontSize: 16)),
          const SizedBox(height: 4),
          Text('단어 뽑기 버튼을 누르면 추천 단어 3개가 생성돼요.', style: TextStyle(color: p.muted, fontSize: 14), textAlign: TextAlign.center),
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
    final int n = words.length;
    final double dividers = n > 1 ? (n - 1) * 1.0 : 0.0;
    final double m = 48.0;
    final double singleBoxH = n > 0 ? n * m + dividers : 0.0;
    const double btnH = 28.8;
    final double spacing = 10.2;
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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                selectedCategory?.name ?? '',
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

          if (pend == null)
            _singleWordsBox(p, boxHeight: singleBoxH)
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
                            rowHeight: compRowH,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: _comparisonWordsBox(
                            p,
                            words,
                            isNew: true,
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

          GoldButton(
            label: pend != null ? '다시 뽑기' : (words.any((w) => w.locked) ? '다시 뽑기' : '전체 다시 뽑기'),
            icon: Icons.casino_outlined,
            onPressed: busy || loading || noneFree ? null : _reroll,
          ),
        ],
      ),
    );
  }

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

  Widget _singleWordsBox(Pal p, {required double boxHeight}) {
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
                child: _wordTile(p, words[i], i),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _wordTile(Pal p, WordItem w, int i) {
    final c = w.rolling ? p.rowIdle : tierColor(context, w.tier);
    final text = w.rolling ? (i < flicker.length && flicker[i].isNotEmpty ? flicker[i] : '…') : w.text;
    final shadow = !w.rolling && glows(w.tier) ? [Shadow(color: c.withValues(alpha: .65), blurRadius: 10)] : null;
    return Material(
      color: w.locked ? p.rowLocked : Colors.transparent,
      child: InkWell(
        onTap: frozen
            ? null
            : () {
                final lockedCount = words.where((item) => item.locked).length;
                if (!w.locked && lockedCount >= 2) {
                  _snack('단어는 최대 2개까지만 고정할 수 있습니다.');
                  return;
                }
                setState(() => w.locked = !w.locked);
              },
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14),
          child: Center(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Icon(w.locked ? Icons.lock : Icons.diamond, size: 20, color: c),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    text,
                    style: TextStyle(color: c, fontSize: 17, fontWeight: FontWeight.w700, shadows: shadow),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(border: Border.all(color: c), borderRadius: BorderRadius.circular(99)),
                  child: Text(
                    w.rolling ? '???' : kTierName[w.tier]!,
                    style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: c),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _comparisonWordsBox(Pal p, List<WordItem> wordList, {required bool isNew, required double rowHeight}) {
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
                child: _compWordTile(p, wordList[i], i, isNew: isNew),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _compWordTile(Pal p, WordItem w, int i, {required bool isNew}) {
    final isRolling = isNew && w.rolling;
    final c = isRolling ? p.rowIdle : tierColor(context, w.tier);
    final text = isRolling ? (i < flicker.length && flicker[i].isNotEmpty ? flicker[i] : '…') : w.text;
    final shadow = !isRolling && glows(w.tier) ? [Shadow(color: c.withValues(alpha: .65), blurRadius: 8)] : null;

    return Container(
      color: w.locked ? p.rowLocked : Colors.transparent,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Center(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Icon(w.locked ? Icons.lock : Icons.diamond, size: 14, color: c),
            const SizedBox(width: 6),
            Expanded(
              child: Text(
                text,
                style: TextStyle(color: c, fontSize: 13, fontWeight: FontWeight.w700, shadows: shadow),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: 4),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
              decoration: BoxDecoration(border: Border.all(color: c, width: 0.8), borderRadius: BorderRadius.circular(99)),
              child: Text(
                isRolling ? '???' : kTierName[w.tier]!,
                style: TextStyle(color: c, fontSize: 9, fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _ideaSection(Pal p) {
    if (idea == null) {
      final msg = loading
          ? 'AI가 아이디어를 조합하고 있어요…'
          : busy
              ? '단어를 뽑는 중이에요'
              : pending != null
                  ? '이전과 이후 중 하나를 선택하면 아이디어를 만들 수 있어요.'
                  : words.isNotEmpty
                      ? '위 버튼을 누르면 단어로 아이디어를 만들어요.'
                      : '단어를 뽑으면 여기에 아이디어가 나와요.';
      return Container(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 14),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: p.line),
        ),
        child: Text(msg, textAlign: TextAlign.center, style: TextStyle(color: p.muted)),
      );
    }

    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      IdeaCard(
        idea: idea!,
        tag: '만들어진 아이디어 · ${selectedCategory?.name ?? ""}',
        onGuide: _showGuide,
        onShare: _shareLink,
        onPost: _postToFeed,
      ),
      const SizedBox(height: 12),
      Row(children: [
        Expanded(
          flex: 10,
          child: SizedBox(
            height: 48,
            child: OutlinedButton(
              onPressed: (frozen && !loading) || busy ? null : _recombine,
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
        Expanded(
          flex: 14,
          child: SizedBox(
            height: 48,
            child: Material(
              color: Colors.transparent,
              child: Ink(
                decoration: BoxDecoration(
                  gradient: saved ? null : kGold,
                  color: saved ? p.accentSoft : null,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: InkWell(
                  borderRadius: BorderRadius.circular(12),
                  onTap: saved ? null : _save,
                  child: Center(
                    child: Text(
                      saved ? '저장됨' : '아이디어 저장',
                      style: TextStyle(
                        color: saved ? p.accent : kGoldInk,
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
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

/* ───────────── 아이디어 기록 화면 ───────────── */

class LogPage extends StatefulWidget {
  const LogPage({super.key});
  @override
  State<LogPage> createState() => _LogPageState();
}

class _LogPageState extends State<LogPage> {
  final StorageService _storage = StorageService();
  bool edit = false;

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
            child: ListenableBuilder(
              listenable: _storage,
              builder: (context, _) => _content(p),
            ),
          ),
        ),
      ),
    );
  }

  Widget _content(Pal p) {
    final items = _storage.records;
    if (items.isEmpty) {
      return ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 24), children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 16),
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(16), border: Border.all(color: p.line, width: 1.5)),
          child: Column(children: [
            Text('아이디어를 생성해보세요', style: TextStyle(color: p.ink, fontWeight: FontWeight.w600)),
            const SizedBox(height: 2),
            Text('홈에서 단어를 뽑고 저장하면 여기에 영구 보관돼요.', style: TextStyle(color: p.muted)),
            const SizedBox(height: 14),
            SizedBox(
              width: 220,
              child: GoldButton(
                label: '아이디어 만들러 가기',
                onPressed: () => Navigator.maybePop(context),
              ),
            ),
          ]),
        ),
      ]);
    }

    return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
      Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
        Text('저장한 아이디어 ${items.length}개', style: TextStyle(color: p.muted, fontSize: 14)),
        TextButton(
          onPressed: () => setState(() => edit = !edit),
          child: Text(edit ? '완료' : '편집', style: TextStyle(color: p.accent, fontWeight: FontWeight.w600)),
        ),
      ]),
      for (final s in items)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            IdeaCard(
              idea: s.idea,
              tag: '${s.categoryName} · ${whenLabel(s.at)}',
              clamp: !edit,
              onGuide: s.guide != null ? () => GuideDialog.show(context, s.guide!) : null,
              onShare: () async {
                await Clipboard.setData(ClipboardData(text: '아이디어: ${s.idea.title}\n${s.idea.content}'));
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('아이디어가 복사되었어요.')));
                }
              },
            ),
            if (edit)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: OutlinedButton(
                  onPressed: () {
                    _storage.remove(s.id);
                    if (_storage.records.isEmpty) setState(() => edit = false);
                  },
                  style: OutlinedButton.styleFrom(
                    foregroundColor: p.danger,
                    side: BorderSide(color: p.danger),
                    minimumSize: const Size.fromHeight(40),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  ),
                  child: const Text('삭제'),
                ),
              ),
          ]),
        ),
    ]);
  }
}
