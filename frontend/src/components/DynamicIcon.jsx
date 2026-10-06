import {
  Award,
  BookOpen,
  Briefcase,
  Camera,
  Clapperboard,
  CodeXml,
  Crown,
  Globe,
  GraduationCap,
  HeartHandshake,
  Megaphone,
  MessageCircle,
  Music,
  Network,
  Palette,
  PenTool,
  Smartphone,
  Sparkles,
  Star,
  Trophy,
  Users,
} from 'lucide-react'

/** Icons referenced by name from the backend (categories, badges). */
const ICONS = {
  Award,
  BookOpen,
  Briefcase,
  Camera,
  Clapperboard,
  Code2: CodeXml,
  CodeXml,
  Crown,
  Globe,
  GraduationCap,
  HeartHandshake,
  Megaphone,
  MessageCircle,
  Music,
  Network,
  Palette,
  PenTool,
  Smartphone,
  Sparkles,
  Star,
  Trophy,
  Users,
}

export default function DynamicIcon({ name, ...props }) {
  const Icon = ICONS[name] || Sparkles
  return <Icon {...props} />
}
