import assert from 'node:assert/strict'

// Three elements suffice to detect walking/allocation before upper-bound rejection.
export async function checkCollectionBounds(z, v3 = false) {
  let parsed = 0
  let walked = 0
  let allocated = 0
  let refined = 0
  const visit = value => { parsed++; return value }
  const element = z.preprocess ? z.preprocess(visit, z.string()) : z.pipe(z.transform(visit), z.string())
  const array = [1, 2, 3]
  const iterator = array[Symbol.iterator].bind(array)
  array[Symbol.iterator] = () => { walked++; return iterator() }
  for (let i = 0; i < 3; i++) {
    Object.defineProperty(array, i, { get() { walked++; return i } })
  }
  const set = new Set([1, 2, 3])
  const values = set.values.bind(set)
  set.values = () => { walked++; return values() }
  set[Symbol.iterator] = () => { walked++; return values() }
  const capArray = n => v3 ? z.array(element).max(n, 'capped') : z.array(element).check(z.maxLength(n, 'capped'))
  const exactArray = v3 ? z.array(element).length(2) : z.array(element).check(z.length(2))
  const capSet = v3 ? z.set(element).size(2) : z.set(element).check(z.size(2))
  const maxSet = v3 ? z.set(element).max(2) : z.set(element).check(z.maxSize(2))
  const cases = [[capArray(2), array], [capArray(0), array], [exactArray, array], [capSet, set], [maxSet, set]]
  if (!v3) {
    const map = new Map([[1, 1], [2, 2], [3, 3]])
    const entries = map[Symbol.iterator].bind(map)
    map[Symbol.iterator] = () => { walked++; return entries() }
    cases.push([z.map(element, element).check(z.maxSize(2)), map])
  }
  for (const [schema, input] of cases) {
    for (const async of [false, true]) {
      const NativeArray = globalThis.Array
      globalThis.Array = new Proxy(NativeArray, {
        apply(target, receiver, args) { allocated++; return Reflect.apply(target, receiver, args) },
        construct(target, args) { allocated++; return Reflect.construct(target, args) },
      })
      let result
      try { result = async ? await z.safeParseAsync?.(schema, input) ?? await schema.safeParseAsync(input)
        : z.safeParse?.(schema, input) ?? schema.safeParse(input) }
      finally { globalThis.Array = NativeArray }
      assert.equal(result.success, false)
      assert.deepEqual({ parsed, walked, allocated }, { parsed: 0, walked: 0, allocated: 0 })
      assert.equal(result.error.issues.length, 1)
      assert.equal(result.error.issues[0].code, 'too_big')
    }
  }
  // Type checks, nested paths, lower bounds, valid transformations, and unbounded collections survive.
  const capped = capArray(2)
  const safe = (schema, input) => z.safeParse?.(schema, input) ?? schema.safeParse(input)
  assert.equal(safe(capped, {}).error.issues[0].code, 'invalid_type')
  const nested = safe(z.object({ items: capped }), { items: array })
  assert.deepEqual(nested.error.issues[0].path, ['items'])
  assert.equal(nested.error.issues[0].message, 'capped')
  const minimum = v3 ? z.array(z.string()).min(2) : z.array(z.string()).check(z.minLength(2))
  assert.equal(safe(minimum, []).error.issues[0].code, 'too_small')
  assert.deepEqual(safe(capped, ['a', 'b']).data, ['a', 'b'])
  const upper = z.pipe ? z.pipe(z.string(), z.transform(value => value.toUpperCase())) : z.string().transform(value => value.toUpperCase())
  assert.deepEqual(safe(z.array(upper), ['a', 'b']).data, ['A', 'B'])
  assert.equal(safe(z.array(z.string()), [1, 2, 3]).error.issues.length, 3)
  if (typeof capped.refine === 'function') {
    safe(capped.refine(() => { refined++; return true }), array)
    assert.equal(refined, 0)
  }
  if (!v3 && typeof capped.overwrite === 'function') {
    assert.deepEqual(z.array(z.number()).overwrite(a => a.slice(0, 2)).max(2).parse([1, 2, 3]), [1, 2])
  }
}
