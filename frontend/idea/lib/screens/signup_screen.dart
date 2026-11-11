import 'package:flutter/material.dart';
import '../services/api_service.dart';

class SignUpScreen extends StatefulWidget {
  final VoidCallback onSuccess;

  const SignUpScreen({super.key, required this.onSuccess});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final _idCtrl = TextEditingController();
  final _pwCtrl = TextEditingController();
  final _pwConfirmCtrl = TextEditingController();
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _birthCtrl = TextEditingController(text: '2000-01-01');

  bool _loading = false;
  String? _errorMsg;

  @override
  void dispose() {
    _idCtrl.dispose();
    _pwCtrl.dispose();
    _pwConfirmCtrl.dispose();
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _birthCtrl.dispose();
    super.dispose();
  }

  Future<void> _handleSignUp() async {
    final id = _idCtrl.text.trim();
    final pw = _pwCtrl.text.trim();
    final pwConfirm = _pwConfirmCtrl.text.trim();
    final name = _nameCtrl.text.trim();
    final email = _emailCtrl.text.trim();
    final birth = _birthCtrl.text.trim();

    if (id.isEmpty || pw.isEmpty || name.isEmpty || email.isEmpty) {
      setState(() => _errorMsg = '모든 필수 항목을 입력해 주세요.');
      return;
    }

    if (pw != pwConfirm) {
      setState(() => _errorMsg = '비밀번호가 일치하지 않습니다.');
      return;
    }

    if (!email.contains('@')) {
      setState(() => _errorMsg = '올바른 이메일 형식을 입력해 주세요.');
      return;
    }

    setState(() {
      _loading = true;
      _errorMsg = null;
    });

    try {
      await ApiService().signUp(
        id: id,
        pw: pw,
        email: email,
        name: name,
        birth: birth.isEmpty ? '2000-01-01' : birth,
      );
      if (mounted) {
        widget.onSuccess();
        Navigator.pop(context);
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('회원가입')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 400),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
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
                      labelText: '아이디 (id)',
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
                      labelText: '비밀번호 (pw)',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _pwConfirmCtrl,
                    enabled: !_loading,
                    obscureText: true,
                    decoration: InputDecoration(
                      labelText: '비밀번호 확인',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _nameCtrl,
                    enabled: !_loading,
                    decoration: InputDecoration(
                      labelText: '이름 (name)',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _emailCtrl,
                    enabled: !_loading,
                    keyboardType: TextInputType.emailAddress,
                    decoration: InputDecoration(
                      labelText: '이메일 (email)',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _birthCtrl,
                    enabled: !_loading,
                    decoration: InputDecoration(
                      labelText: '생년월일 (YYYY-MM-DD)',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  SizedBox(
                    height: 50,
                    child: FilledButton(
                      onPressed: _loading ? null : _handleSignUp,
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
                              '가입 완료',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                    ),
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
