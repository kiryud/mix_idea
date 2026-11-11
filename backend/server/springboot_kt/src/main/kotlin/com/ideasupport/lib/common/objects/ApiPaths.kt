package com.ideasupport.lib.common.objects

object ApiPaths {
	// server
	const val LOCALHOST = "http://localhost:"

	// common
	private const val COMMON = "/common"

	// test
	const val HEALTH = "$COMMON/health"
	const val ERROR = "$COMMON/error"
	const val USER_LEVEL_LOCK = "$COMMON/user-level-lock"

	// ip
	private const val IP = "$COMMON/ip"
	const val INIT_IP = "$IP/init"

	// app
	private const val APP = "/app"

	// dev
	private const val DEV = "$APP/dev"
	const val GET_PW = "$DEV/get-pw/{pw}"

	// user
	private const val USER = "$APP/user"
	const val PROFILE = "$USER/profile"
	const val SIGN_UP = "$USER/sign-up"
	const val LOGIN = "$USER/login"
	const val REFRESH_TOKEN = "$USER/refresh-token"

	const val CATEGORIES = "$APP/categories"
	const val CATEGORY_PREVIEW = "$CATEGORIES/preview"
	const val DRAW_WORDS = "$APP/words/draw"
	const val IDEAS = "$APP/ideas"
	const val IDEA = "$IDEAS/{id}"
	const val RECOMBINE_IDEA = "$IDEA/recombine"
	const val IDEA_RECIPE = "$IDEA/recipe"
	const val IDEA_GUIDE = "$IDEA/guide"
	const val SHARES = "$APP/shares"
	const val POSTS = "$APP/posts"
	const val POST_REACTION = "$POSTS/{id}/reaction"
	const val PUBLIC_SHARE = "/s/{code}"

	// admin
	private const val ADMIN = "/admin"
}
