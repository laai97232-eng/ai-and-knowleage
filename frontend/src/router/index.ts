import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
    { path: '/register', component: () => import('../views/Register.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('../layouts/StudentLayout.vue'),
      children: [
        { path: '', component: () => import('../views/student/Home.vue'), meta: { active: '/' } },
        { path: 'courses', component: () => import('../views/student/Courses.vue'), meta: { active: '/courses' } },
        { path: 'chat', component: () => import('../views/student/Chat.vue'), meta: { active: '/chat' } },
        { path: 'graph', component: () => import('../views/student/Graph.vue'), meta: { active: '/graph' } },
        { path: 'insights', component: () => import('../views/student/Insights.vue'), meta: { active: '/insights' } },
        { path: 'courses/:id', component: () => import('../views/student/CourseDetail.vue'), meta: { active: '/courses' } },
        { path: 'knowledge/:id', component: () => import('../views/student/Knowledge.vue'), meta: { active: '/courses' } },
        { path: 'practice/:id', component: () => import('../views/student/Practice.vue'), meta: { active: '/courses' } },
        { path: 'plans', component: () => import('../views/student/Plans.vue'), meta: { active: '/plans' } },
        { path: 'plans/:id', component: () => import('../views/student/PlanDetail.vue'), meta: { active: '/plans' } },
        { path: 'records', component: () => import('../views/student/Records.vue'), meta: { active: '/records' } },
        { path: 'wrong', component: () => import('../views/student/Wrong.vue'), meta: { active: '/wrong' } },
        { path: 'profile', component: () => import('../views/student/Profile.vue'), meta: { active: '/profile' } }
      ]
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      children: [
        { path: '', component: () => import('../views/admin/Dashboard.vue'), meta: { active: '/admin' } },
        { path: 'users', component: () => import('../views/admin/Users.vue'), meta: { active: '/admin/users' } },
        { path: 'courses', component: () => import('../views/admin/Courses.vue'), meta: { active: '/admin/courses' } },
        { path: 'courses/:id', component: () => import('../views/admin/CourseEdit.vue'), meta: { active: '/admin/courses' } },
        { path: 'relations', component: () => import('../views/admin/Relations.vue'), meta: { active: '/admin/relations' } },
        { path: 'documents', component: () => import('../views/admin/Documents.vue'), meta: { active: '/admin/documents' } },
        { path: 'questions', component: () => import('../views/admin/Questions.vue'), meta: { active: '/admin/questions' } }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.meta.public) {
    return true
  }
  if (!store.token) {
    return '/login'
  }
  if (!store.profile) {
    try {
      await store.fetchMe()
    } catch {
      store.logout()
      return '/login'
    }
  }
  const role = store.profile?.role
  const adminPath = to.path.startsWith('/admin')
  if (adminPath && role !== 'ADMIN') return '/'
  if (!adminPath && role === 'ADMIN') return '/admin'
  return true
})

export default router
