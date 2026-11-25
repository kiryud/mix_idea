import { useState } from 'react';

export default function AuthPanel({ api, run, busy, onLogin }) {
	const [signup, setSignup] = useState(false);
	const [form, setForm] = useState({ id: '', pw: '', email: '', name: '', birth: '2000-01-01' });
	const field = (key) => ({
		value: form[key],
		onChange: (event) => setForm({ ...form, [key]: event.target.value }),
	});
	async function submit(event) {
		event.preventDefault();
		await run(
			async () => {
				if (signup)
					await api.request('/app/user/sign-up', {
						method: 'POST',
						body: form,
						auth: false,
					});
				const tokens = await api.request('/app/user/login', {
					method: 'POST',
					body: { id: form.id, pw: form.pw },
					auth: false,
				});
				api.setSession(tokens);
				const profile = await api.request('/app/user/profile');
				setForm({ ...form, pw: '' });
				onLogin(profile);
			},
			signup ? '회원가입과 로그인 완료. 첫 아이디어를 만들어 보세요.' : '로그인했습니다.',
		);
	}
	return (
		<div className="auth-layout">
			<div className="auth-story">
				<span className="eyebrow">LET’S MAKE SOMETHING</span>
				<h2>
					세 단어가 만나면
					<br />
					새로운 생각이 됩니다<span>.</span>
				</h2>
				<p>
					고르고, 섞고, 만들어 보세요.
					<br />
					실제 백엔드와 연결된 학생용 아이디어 작업실입니다.
				</p>
				<div className="word-art">
					<span>감자</span>
					<b>＋</b>
					<span>캠핑</span>
					<b>＋</b>
					<span>치즈</span>
				</div>
			</div>
			<form className="card auth-card" onSubmit={submit}>
				<span className="step-kicker">시작하기</span>
				<h2>{signup ? '내 작업실 만들기' : '작업실에 입장하기'}</h2>
				<p className="muted">기존 회원 인증을 그대로 사용합니다.</p>
				<label>
					아이디
					<input
						autoComplete="username"
						required
						{...field('id')}
						placeholder="사용할 아이디"
					/>
				</label>
				<label>
					비밀번호
					<input
						type="password"
						autoComplete={signup ? 'new-password' : 'current-password'}
						required
						{...field('pw')}
						placeholder="비밀번호"
					/>
				</label>
				{signup && (
					<>
						<label>
							이름
							<input required {...field('name')} placeholder="화면에 표시할 이름" />
						</label>
						<label>
							이메일
							<input
								type="email"
								required
								{...field('email')}
								placeholder="student@example.com"
							/>
						</label>
						<label>
							생년월일
							<input type="date" required {...field('birth')} />
						</label>
					</>
				)}
				<button className="primary full" disabled={!!busy}>
					{busy ? '연결 중…' : signup ? '가입하고 시작하기' : '로그인'}
				</button>
				<button
					type="button"
					className="text-button"
					disabled={!!busy}
					onClick={() => setSignup(!signup)}
				>
					{signup ? '이미 계정이 있나요? 로그인' : '처음인가요? 회원가입'}
				</button>
			</form>
		</div>
	);
}
