import 'package:flutter/material.dart';
import '../services/api_service.dart';
import 'signup_screen.dart';

class LoginScreen extends StatefulWidget {
  final VoidCallback onLoginSuccess;
  final VoidCallback onGuestMode;

  const LoginScreen({
    super.key,
    required this.onLoginSuccess,
    required this.onGuestMode,
  });

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _idCtrl = TextEditingController(text: 'student01');
  final _pwCtrl = TextEditingController(text: 'DEMO_PASSWORD');
  bool _loading = false;
  String? _errorMsg;

  @override
  void dispose() {
    _idCtrl.dispose();
    _pwCtrl.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    final id = _idCtrl.text.trim();
    final pw = _pwCtrl.text.trim();
    if (id.isEmpty || pw.isEmpty) {
      setState(() => _errorMsg = '아이디와 비밀번호를 모두 입력해 주세요.');
      return;
    }

    setState(() {
      _loading = true;
      _errorMsg = null;
    });

    try {
      await ApiService().login(id: id, pw: pw);
      if (mounted) {
        widget.onLoginSuccess();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMsg = e.toString();
          _loading = false;
        });
      }
    }
  }

  void _showServerSettings() {
    final api = ApiService();
    final ctrl = TextEditingController(text: api.baseUrl);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('백엔드 서버 주소 설정'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '스프링부트 서버 API URL을 입력하세요:\n(Web/Desktop: http://localhost:8080)\n(Android 애뮬레이터: http://10.0.2.2:8080)',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: ctrl,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: 'http://localhost:8080',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('취소'),
          ),
          FilledButton(
            onPressed: () async {
              await api.setBaseUrl(ctrl.text.trim());
              if (ctx.mounted) {
                Navigator.pop(ctx);
              }
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('서버 주소 저장: ${api.baseUrl}')),
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
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('아이디어 뽑기 로그인'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            tooltip: '서버 주소 설정',
            onPressed: _showServerSettings,
          ),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 400),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Icon(
                    Icons.lightbulb_outline,
                    size: 64,
                    color: theme.colorScheme.primary,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    '아이디어 뽑기 & 기획',
                    textAlign: TextAlign.center,
                    style: theme.textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '스프링부트 백엔드와 연동하여 AI 기획을 시작하세요',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: isDark ? Colors.grey[400] : Colors.grey[600],
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(height: 28),

                  if (_errorMsg != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.red.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: Colors.red.withValues(alpha: 0.4)),
                      ),
                      child: Text(
                        _errorMsg!,
                        style: const TextStyle(color: Colors.red, fontSize: 13),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],

                  TextField(
                    controller: _idCtrl,
                    enabled: !_loading,
                    decoration: InputDecoration(
                      labelText: '아이디',
                      prefixIcon: const Icon(Icons.person_outline),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _pwCtrl,
                    enabled: !_loading,
                    obscureText: true,
                    decoration: InputDecoration(
                      labelText: '비밀번호',
                      prefixIcon: const Icon(Icons.lock_outline),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    onSubmitted: (_) => _handleLogin(),
                  ),
                  const SizedBox(height: 20),

                  SizedBox(
                    height: 50,
                    child: FilledButton(
                      onPressed: _loading ? null : _handleLogin,
                      style: FilledButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: _loading
                          ? const SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                strokeWidth: 2.5,
                                color: Colors.white,
                              ),
                            )
                          : const Text(
                              '로그인',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                    ),
                  ),
                  const SizedBox(height: 12),

                  OutlinedButton(
                    onPressed: _loading
                        ? null
                        : () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => SignUpScreen(
                                  onSuccess: () {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(
                                        content: Text(
                                            '회원가입이 완료되었습니다. 로그인해 주세요.'),
                                      ),
                                    );
                                  },
                                ),
                              ),
                            );
                          },
                    style: OutlinedButton.styleFrom(
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                    ),
                    child: const Text('회원가입'),
                  ),
                  const SizedBox(height: 12),

                  TextButton(
                    onPressed: _loading ? null : widget.onGuestMode,
                    child: const Text('로그인 없이 게스트로 둘러보기 (오프라인 모드)'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
