import test from 'node:test'
import assert from 'node:assert/strict'
import { decimalToScaledInteger, rupeesToPaise } from './currency.js'

test('converts decimal rupees to exact paise without binary floating point rounding', () => {
  assert.equal(rupeesToPaise('1.05'), 105)
  assert.equal(rupeesToPaise('1.5'), 150)
  assert.equal(rupeesToPaise('0.01'), 1)
  assert.equal(rupeesToPaise('123'), 12300)
})

test('rejects fractions of a paise and unsafe amounts', () => {
  assert.throws(() => rupeesToPaise('1.005'))
  assert.throws(() => rupeesToPaise('-1'))
  assert.throws(() => rupeesToPaise('90071992547410'))
})

test('converts percentage strings to exact basis points', () => {
  assert.equal(decimalToScaledInteger('12.34', 2), 1234)
  assert.throws(() => decimalToScaledInteger('12.345', 2))
})
