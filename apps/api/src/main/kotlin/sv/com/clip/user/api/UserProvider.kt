package sv.com.clip.user.api

interface UserProvider {
  fun getAuthViewByUsername(username: String): UserAuthView?
}
