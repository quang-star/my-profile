import {
  ArrowUpRight, BookOpen, Briefcase, Code2, Database, Globe, Layers, Mail, MapPin, Sparkles, Star, Zap,
} from '@lucide/vue'
import SocialIcon from '@/components/SocialIcon.vue'

export const nodeIcons = {
  github: { label: 'GitHub', component: SocialIcon },
  code: { label: 'Code', component: Code2 },
  mail: { label: 'Email', component: Mail },
  sparkles: { label: 'Sparkles', component: Sparkles },
  star: { label: 'Star', component: Star },
  zap: { label: 'Lightning', component: Zap },
  briefcase: { label: 'Briefcase', component: Briefcase },
  book: { label: 'Book', component: BookOpen },
  database: { label: 'Database', component: Database },
  layers: { label: 'Layers', component: Layers },
  globe: { label: 'Globe', component: Globe },
  location: { label: 'Location', component: MapPin },
  link: { label: 'External link', component: ArrowUpRight },
}

export function getNodeIcon(name?: string) {
  return name && Object.hasOwn(nodeIcons, name) ? nodeIcons[name as keyof typeof nodeIcons] : undefined
}
