import 'dart:math';

enum Tier { rare, epic, unique, legend, mystic }

const Map<Tier, String> kTierName = {
  Tier.rare: '레어',
  Tier.epic: '에픽',
  Tier.unique: '유니크',
  Tier.legend: '레전드리',
  Tier.mystic: '미스틱',
};

int rank(Tier t) => t.index + 1;
bool glows(Tier t) => rank(t) >= 3;

Tier randTier([Random? rnd]) => parseTier(null, rnd);

Tier parseTier(String? val, [Random? rnd]) {

  if (val != null) {
    switch (val.toLowerCase()) {
      case 'rare':
        return Tier.rare;
      case 'epic':
        return Tier.epic;
      case 'unique':
        return Tier.unique;
      case 'legend':
        return Tier.legend;
      case 'mystic':
        return Tier.mystic;
    }
  }
  // 서버에 tier가 없거나 오프라인일 때 확률 추출
  final r = rnd ?? Random();
  final x = r.nextDouble();
  if (x < .52) return Tier.rare;
  if (x < .80) return Tier.epic;
  if (x < .94) return Tier.unique;
  if (x < .98) return Tier.legend;
  return Tier.mystic;
}

Tier bestOf(Iterable<Tier> ts) =>
    ts.isEmpty ? Tier.rare : ts.reduce((a, b) => rank(b) > rank(a) ? b : a);

class CategoryItem {
  final String id;
  final String name;
  final String kind; // 'default' | 'custom'
  final int wordCount;
  final int useCount;
  final String guideType; // 'food' | 'software' | 'contest' | 'general'
  final String guideLabel;

  const CategoryItem({
    required this.id,
    required this.name,
    this.kind = 'default',
    this.wordCount = 0,
    this.useCount = 0,
    this.guideType = 'general',
    this.guideLabel = '실행 계획',
  });

  factory CategoryItem.fromJson(Map<String, dynamic> json) {
    return CategoryItem(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      kind: json['kind'] as String? ?? 'default',
      wordCount: (json['wordCount'] as num?)?.toInt() ?? 0,
      useCount: (json['useCount'] as num?)?.toInt() ?? 0,
      guideType: json['guideType'] as String? ?? 'general',
      guideLabel: json['guideLabel'] as String? ?? '실행 계획',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'kind': kind,
        'wordCount': wordCount,
        'useCount': useCount,
        'guideType': guideType,
        'guideLabel': guideLabel,
      };
}

class WordItem {
  final String id;
  String text;
  Tier tier;
  bool locked;
  bool rolling;

  WordItem({
    required this.id,
    required this.text,
    this.tier = Tier.rare,
    this.locked = false,
    this.rolling = false,
  });

  WordItem copy() => WordItem(
        id: id,
        text: text,
        tier: tier,
        locked: locked,
        rolling: rolling,
      );

  factory WordItem.fromJson(Map<String, dynamic> json) {
    return WordItem(
      id: json['id'] as String? ?? '',
      text: json['text'] as String? ?? '',
      locked: json['locked'] as bool? ?? false,
      tier: parseTier(json['tier'] as String?),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'text': text,
        'tier': tier.name,
        'locked': locked,
      };
}

class IdeaItem {
  final String id;
  final String categoryId;
  final String categoryName;
  final String title;
  final String content;
  final List<WordItem> words;
  final Tier grade;
  final String guideType;
  final String guideLabel;
  final String source; // 'gemini' | 'mock'
  final DateTime createdAt;
  final bool hasGuide;
  final String? shareUrl;

  const IdeaItem({
    required this.id,
    required this.categoryId,
    required this.categoryName,
    required this.title,
    required this.content,
    required this.words,
    required this.grade,
    this.guideType = 'general',
    this.guideLabel = '실행 계획',
    this.source = 'mock',
    required this.createdAt,
    this.hasGuide = false,
    this.shareUrl,
  });

  factory IdeaItem.fromJson(Map<String, dynamic> json, {String? categoryName}) {
    final rawWords = json['words'] as List<dynamic>? ?? [];
    final wordList = <WordItem>[];
    for (final w in rawWords) {
      if (w is Map<String, dynamic>) {
        wordList.add(WordItem.fromJson(w));
      } else if (w is String) {
        wordList.add(WordItem(id: '', text: w, tier: Tier.rare));
      }
    }
    final rawGrade = json['grade'] as String?;
    final grade = rawGrade != null
        ? parseTier(rawGrade)
        : bestOf(wordList.map((w) => w.tier));

    DateTime created;
    try {
      created = json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now();
    } catch (_) {
      created = DateTime.now();
    }

    return IdeaItem(
      id: json['id'] as String? ?? '',
      categoryId: json['categoryId'] as String? ?? '',
      categoryName: categoryName ?? (json['categoryName'] as String? ?? ''),
      title: json['title'] as String? ?? '',
      content: json['content'] as String? ?? '',
      words: wordList,
      grade: grade,
      guideType: json['guideType'] as String? ?? 'general',
      guideLabel: json['guideLabel'] as String? ?? '실행 계획',
      source: json['source'] as String? ?? 'gemini',
      createdAt: created,
      hasGuide: json['hasGuide'] as bool? ?? false,
      shareUrl: json['shareUrl'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'categoryId': categoryId,
        'categoryName': categoryName,
        'title': title,
        'content': content,
        'words': words.map((w) => w.toJson()).toList(),
        'grade': grade.name,
        'guideType': guideType,
        'guideLabel': guideLabel,
        'source': source,
        'createdAt': createdAt.toIso8601String(),
        'hasGuide': hasGuide,
        'shareUrl': shareUrl,
      };
}

class GuideRequirement {
  final String name;
  final String detail;
  const GuideRequirement({required this.name, required this.detail});

  factory GuideRequirement.fromJson(Map<String, dynamic> json) =>
      GuideRequirement(
        name: json['name'] as String? ?? '',
        detail: json['detail'] as String? ?? '',
      );

  Map<String, dynamic> toJson() => {'name': name, 'detail': detail};
}

class GuideStep {
  final int order;
  final String title;
  final String description;
  const GuideStep(
      {required this.order, required this.title, required this.description});

  factory GuideStep.fromJson(Map<String, dynamic> json) => GuideStep(
        order: (json['order'] as num?)?.toInt() ?? 1,
        title: json['title'] as String? ?? '',
        description: json['description'] as String? ?? '',
      );

  Map<String, dynamic> toJson() =>
      {'order': order, 'title': title, 'description': description};
}

class GuideItem {
  final String ideaId;
  final String kind;
  final String label;
  final String requirementsLabel;
  final bool feasible;
  final String summary;
  final List<GuideRequirement> requirements;
  final List<GuideStep> steps;
  final String source;

  const GuideItem({
    required this.ideaId,
    required this.kind,
    required this.label,
    required this.requirementsLabel,
    required this.feasible,
    required this.summary,
    required this.requirements,
    required this.steps,
    required this.source,
  });

  factory GuideItem.fromJson(Map<String, dynamic> json) {
    return GuideItem(
      ideaId: json['ideaId'] as String? ?? '',
      kind: json['kind'] as String? ?? 'general',
      label: json['label'] as String? ?? '실행 계획',
      requirementsLabel: json['requirementsLabel'] as String? ?? '필요한 것',
      feasible: json['feasible'] as bool? ?? true,
      summary: json['summary'] as String? ?? '',
      requirements: (json['requirements'] as List<dynamic>? ?? [])
          .map((r) => GuideRequirement.fromJson(r as Map<String, dynamic>))
          .toList(),
      steps: (json['steps'] as List<dynamic>? ?? [])
          .map((s) => GuideStep.fromJson(s as Map<String, dynamic>))
          .toList(),
      source: json['source'] as String? ?? 'gemini',
    );
  }

  Map<String, dynamic> toJson() => {
        'ideaId': ideaId,
        'kind': kind,
        'label': label,
        'requirementsLabel': requirementsLabel,
        'feasible': feasible,
        'summary': summary,
        'requirements': requirements.map((r) => r.toJson()).toList(),
        'steps': steps.map((s) => s.toJson()).toList(),
        'source': source,
      };
}

class ReactionCounts {
  final int like;
  final int meh;
  final int dislike;
  const ReactionCounts({this.like = 0, this.meh = 0, this.dislike = 0});

  factory ReactionCounts.fromJson(Map<String, dynamic> json) => ReactionCounts(
        like: (json['like'] as num?)?.toInt() ?? 0,
        meh: (json['meh'] as num?)?.toInt() ?? 0,
        dislike: (json['dislike'] as num?)?.toInt() ?? 0,
      );

  Map<String, dynamic> toJson() => {
        'like': like,
        'meh': meh,
        'dislike': dislike,
      };
}

class PostItem {
  final String id;
  final String ideaId;
  final String title;
  final String content;
  final List<String> words;
  final String authorName;
  final bool isMine;
  final String? myReaction;
  final ReactionCounts reactions;
  final DateTime createdAt;

  const PostItem({
    required this.id,
    required this.ideaId,
    required this.title,
    required this.content,
    required this.words,
    required this.authorName,
    required this.isMine,
    this.myReaction,
    required this.reactions,
    required this.createdAt,
  });

  factory PostItem.fromJson(Map<String, dynamic> json) {
    DateTime created;
    try {
      created = json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now();
    } catch (_) {
      created = DateTime.now();
    }

    return PostItem(
      id: json['id'] as String? ?? '',
      ideaId: json['ideaId'] as String? ?? '',
      title: json['title'] as String? ?? '',
      content: json['content'] as String? ?? '',
      words: (json['words'] as List<dynamic>? ?? []).map((e) => e.toString()).toList(),
      authorName: json['authorName'] as String? ?? '',
      isMine: json['isMine'] as bool? ?? false,
      myReaction: json['myReaction'] as String?,
      reactions: json['reactions'] != null
          ? ReactionCounts.fromJson(json['reactions'] as Map<String, dynamic>)
          : const ReactionCounts(),
      createdAt: created,
    );
  }
}

class UserSession {
  final String? uid;
  final String id;
  final String name;
  final String email;
  final String accessToken;
  final String refreshToken;

  const UserSession({
    this.uid,
    required this.id,
    required this.name,
    required this.email,
    required this.accessToken,
    required this.refreshToken,
  });

  factory UserSession.fromJson(Map<String, dynamic> json) => UserSession(
        uid: json['uid'] as String?,
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        email: json['email'] as String? ?? '',
        accessToken: json['accessToken'] as String? ?? '',
        refreshToken: json['refreshToken'] as String? ?? '',
      );

  Map<String, dynamic> toJson() => {
        'uid': uid,
        'id': id,
        'name': name,
        'email': email,
        'accessToken': accessToken,
        'refreshToken': refreshToken,
      };
}
