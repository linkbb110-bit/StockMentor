const BCRYPT_MAX_UTF8_BYTES = 72

const isWithinApprovedCharacterLength = (password: string): boolean =>
  password.length >= 8 && password.length <= 64

export const isBcryptCompatiblePassword = (password: string): boolean =>
  new TextEncoder().encode(password).length <= BCRYPT_MAX_UTF8_BYTES

export const isValidLoginPassword = (password: string): boolean =>
  isWithinApprovedCharacterLength(password) && isBcryptCompatiblePassword(password)

export const isValidRegistrationPassword = (password: string): boolean =>
  isValidLoginPassword(password) && /[A-Za-z]/.test(password) && /[0-9]/.test(password)
