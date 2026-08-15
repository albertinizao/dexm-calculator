import type { GrenadeCatalogItem, WeaponCatalogItem } from './api';

export type StaticAbility = { name: string; description?: string; launchType?: string; cost?: number | null; uniqueFlag?: string; alternativesJson?: string };
type ObjectsCatalog = { version: string; weapons: WeaponCatalogItem[]; grenades: GrenadeCatalogItem[]; armors: unknown[]; shields: unknown[]; physicalShields: unknown[] };
let abilitiesPromise: Promise<StaticAbility[]> | undefined;
let objectsPromise: Promise<ObjectsCatalog> | undefined;
export function loadStaticAbilities() {
  abilitiesPromise ??= fetch('/catalogs/abilities.v1.json', { cache: 'force-cache' }).then(async response => {
    if (!response.ok) throw new Error('No se pudo cargar el catálogo de habilidades.');
    const raw = await response.json() as Array<Record<string, unknown>>;
    return raw.map(item => ({ name: String(item.Nombre ?? ''), description: item.Descripcion as string | undefined, launchType: item.Lanzamiento as string | undefined, cost: item.Coste as number | null | undefined, uniqueFlag: item.Unica as string | undefined, alternativesJson: JSON.stringify([item]) })).filter(item => item.name);
  });
  return abilitiesPromise;
}
export function loadStaticObjects() {
  objectsPromise ??= fetch('/catalogs/objects.v1.json', { cache: 'force-cache' }).then(async response => {
    if (!response.ok) throw new Error('No se pudo cargar el catálogo de objetos.');
    const value = await response.json() as ObjectsCatalog;
    return value;
  });
  return objectsPromise;
}
