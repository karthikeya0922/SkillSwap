import { useEffect, useMemo, useState } from 'react'
import { skillService } from '../services'
import { Select } from './ui/Field'

let catalogPromise = null
/** Skills rarely change during a session, so the catalogue is fetched once and shared. */
export function loadSkillCatalog({ refresh = false } = {}) {
  if (!catalogPromise || refresh) {
    catalogPromise = skillService.skills().catch((e) => {
      catalogPromise = null
      throw e
    })
  }
  return catalogPromise
}

export function useSkillCatalog() {
  const [skills, setSkills] = useState([])
  useEffect(() => {
    loadSkillCatalog().then(setSkills).catch(() => setSkills([]))
  }, [])
  return skills
}

/** <select> of skills grouped by category. `only` limits the choices to a set of skill ids. */
export default function SkillPicker({ value, onChange, only, placeholder = 'Choose a skill', error, ...rest }) {
  const skills = useSkillCatalog()
  const groups = useMemo(() => {
    const filtered = only ? skills.filter((s) => only.includes(s.id)) : skills
    const map = new Map()
    filtered.forEach((s) => {
      if (!map.has(s.categoryName)) map.set(s.categoryName, [])
      map.get(s.categoryName).push(s)
    })
    return [...map.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [skills, only])

  return (
    <Select value={value ?? ''} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)} error={error} {...rest}>
      <option value="">{placeholder}</option>
      {groups.map(([category, list]) => (
        <optgroup key={category} label={category}>
          {list.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </optgroup>
      ))}
    </Select>
  )
}
