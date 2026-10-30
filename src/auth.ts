import NextAuth from "next-auth";
import Google from "next-auth/providers/google";

// JWT sessions (no DB adapter): the user id is Google's stable `sub`.
export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [Google],
  callbacks: {
    session({ session, token }) {
      if (token.sub) session.user.id = token.sub;
      return session;
    },
  },
});
