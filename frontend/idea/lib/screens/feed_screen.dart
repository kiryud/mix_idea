import 'package:flutter/material.dart';
import '../models/models.dart';
import '../services/api_service.dart';

class FeedScreen extends StatefulWidget {
  const FeedScreen({super.key});

  @override
  State<FeedScreen> createState() => _FeedScreenState();
}

class _FeedScreenState extends State<FeedScreen> {
  final ApiService _api = ApiService();
  List<PostItem> _posts = [];
  bool _loading = false;
  String? _error;
  String _sort = 'latest'; // 'latest' | 'popular'

  @override
  void initState() {
    super.initState();
    _loadPosts();
  }

  Future<void> _loadPosts() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final posts = await _api.getPosts(sort: _sort, limit: 30);
      if (mounted) {
        setState(() {
          _posts = posts;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _loading = false;
        });
      }
    }
  }

  Future<void> _handleReaction(PostItem post, String kind) async {
    if (post.isMine) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('자신이 작성한 글에는 반응할 수 없습니다.')),
      );
      return;
    }

    final newKind = post.myReaction == kind ? null : kind;
    try {
      await _api.reactPost(post.id, newKind);
      await _loadPosts();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('반응 변경 실패: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('아이디어 피드'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loading ? null : _loadPosts,
          ),
        ],
      ),
      body: Column(
        children: [
          // 정렬 선택
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                ChoiceChip(
                  label: const Text('최신순'),
                  selected: _sort == 'latest',
                  onSelected: (v) {
                    if (v && _sort != 'latest') {
                      setState(() => _sort = 'latest');
                      _loadPosts();
                    }
                  },
                ),
                const SizedBox(width: 8),
                ChoiceChip(
                  label: const Text('인기순'),
                  selected: _sort == 'popular',
                  onSelected: (v) {
                    if (v && _sort != 'popular') {
                      setState(() => _sort = 'popular');
                      _loadPosts();
                    }
                  },
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                _error!,
                                textAlign: TextAlign.center,
                                style: const TextStyle(color: Colors.red),
                              ),
                              const SizedBox(height: 12),
                              FilledButton(
                                onPressed: _loadPosts,
                                child: const Text('다시 시도'),
                              ),
                            ],
                          ),
                        ),
                      )
                    : _posts.isEmpty
                        ? Center(
                            child: Text(
                              '공유된 아이디어가 아직 없습니다.\n홈에서 아이디어를 만들어 피드에 올려보세요!',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                color: isDark ? Colors.grey[400] : Colors.grey[600],
                              ),
                            ),
                          )
                        : ListView.separated(
                            padding: const EdgeInsets.all(16),
                            itemCount: _posts.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 12),
                            itemBuilder: (ctx, i) {
                              final p = _posts[i];
                              return _buildPostCard(theme, isDark, p);
                            },
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildPostCard(ThemeData theme, bool isDark, PostItem post) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: isDark ? Colors.grey[800]! : Colors.grey[300]!,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    CircleAvatar(
                      radius: 12,
                      backgroundColor: theme.colorScheme.primary.withValues(alpha: 0.2),
                      child: Text(
                        post.authorName.isNotEmpty ? post.authorName[0] : '?',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: theme.colorScheme.primary,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      post.authorName,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                    if (post.isMine) ...[
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 1),
                        decoration: BoxDecoration(
                          color: theme.colorScheme.primary.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          '내 글',
                          style: TextStyle(
                            fontSize: 10,
                            color: theme.colorScheme.primary,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
                Text(
                  '${post.createdAt.month}월 ${post.createdAt.day}일',
                  style: TextStyle(
                    fontSize: 11,
                    color: isDark ? Colors.grey[500] : Colors.grey[600],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),

            // 키워드
            if (post.words.isNotEmpty)
              Wrap(
                spacing: 6,
                runSpacing: 4,
                children: post.words
                    .map(
                      (w) => Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: theme.colorScheme.primary.withValues(alpha: 0.08),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          '#$w',
                          style: TextStyle(
                            fontSize: 12,
                            color: theme.colorScheme.primary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    )
                    .toList(),
              ),
            const SizedBox(height: 8),

            Text(
              post.title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              post.content,
              style: TextStyle(
                fontSize: 14,
                height: 1.45,
                color: isDark ? Colors.grey[300] : Colors.grey[800],
              ),
            ),
            const SizedBox(height: 14),

            // 반응 버튼 영역 (like, meh, dislike)
            Row(
              children: [
                _buildReactionBtn(
                  icon: Icons.thumb_up_alt_outlined,
                  activeIcon: Icons.thumb_up,
                  label: '${post.reactions.like}',
                  selected: post.myReaction == 'like',
                  color: Colors.blue,
                  onTap: () => _handleReaction(post, 'like'),
                ),
                const SizedBox(width: 8),
                _buildReactionBtn(
                  icon: Icons.sentiment_neutral_outlined,
                  activeIcon: Icons.sentiment_neutral,
                  label: '${post.reactions.meh}',
                  selected: post.myReaction == 'meh',
                  color: Colors.amber,
                  onTap: () => _handleReaction(post, 'meh'),
                ),
                const SizedBox(width: 8),
                _buildReactionBtn(
                  icon: Icons.thumb_down_alt_outlined,
                  activeIcon: Icons.thumb_down,
                  label: '${post.reactions.dislike}',
                  selected: post.myReaction == 'dislike',
                  color: Colors.red,
                  onTap: () => _handleReaction(post, 'dislike'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildReactionBtn({
    required IconData icon,
    required IconData activeIcon,
    required String label,
    required bool selected,
    required Color color,
    required VoidCallback onTap,
  }) {
    return InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: selected ? color.withValues(alpha: 0.15) : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: selected ? color : Colors.grey.withValues(alpha: 0.3),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              selected ? activeIcon : icon,
              size: 16,
              color: selected ? color : Colors.grey,
            ),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: selected ? FontWeight.bold : FontWeight.normal,
                color: selected ? color : Colors.grey,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
