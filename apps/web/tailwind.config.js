const { createGlobPatternsForDependencies } = require('@nx/angular/tailwind');
const { join } = require('path');

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    join(__dirname, 'src/**/!(*.stories|*.spec).{ts,html}'),
    ...createGlobPatternsForDependencies(__dirname),
  ],
  darkMode: 'class',
  theme: {
    extend: {


      // ── Colores ────────────────────────────────────────────────────
      colors: {

        // Vocabulario — paleta Wong (los valores reales viven en styles.scss)
        'word-unknown':    'var(--color-word-unknown)',
        'word-new':        'var(--color-word-new)',
        'word-recognized': 'var(--color-word-recognized)',
        'word-familiar':   'var(--color-word-familiar)',
        'word-learned':    'var(--color-word-learned)',
        'word-not-found':  'var(--color-word-not-found)',
        'word-ignored':    'var(--color-word-ignored)',
        'karaoke':         'var(--color-karaoke)',

        // Superficies
        'surface': {
          DEFAULT:   'var(--color-surface)',
          secondary: 'var(--color-bg-secondary)',
          border:    'var(--color-border)',
        },

        // Texto semántico
        'content': {
          primary:   'var(--color-text-primary)',
          secondary: 'var(--color-text-secondary)',
        },

        // Marca y foco
        'brand':  'var(--color-brand)',
        'focus':  'var(--color-focus-ring)',
      },

      // ── Tipografía ─────────────────────────────────────────────────
      fontFamily: {
        'ui':     ['Atkinson Hyperlegible', 'system-ui', 'sans-serif'],
        'reader': ['Atkinson Hyperlegible', 'Georgia', 'serif'],
      },

      fontSize: {
        'xs':     ['0.75rem',  { lineHeight: '1rem' }],
        'sm':     ['0.875rem', { lineHeight: '1.25rem' }],
        'base':   ['1rem',     { lineHeight: '1.5rem' }],
        'lg':     ['1.125rem', { lineHeight: '1.75rem' }],
        'xl':     ['1.25rem',  { lineHeight: '1.75rem' }],
        '2xl':    ['1.5rem',   { lineHeight: '2rem' }],
        'reader': ['1.125rem', { lineHeight: '1.9rem' }],  // más interlineado para lectura larga
      },

      // ── Layout ─────────────────────────────────────────────────────
      maxWidth: {
        'reader': '680px',   // ancho editorial del lector
        'page':   '1200px',  // ancho máximo de página general
      },

      // ── Bordes ─────────────────────────────────────────────────────
      borderRadius: {
        'sm':  '4px',
        DEFAULT: '6px',
        'md':  '8px',
        'lg':  '12px',
      },

      // ── Sombras ────────────────────────────────────────────────────
      boxShadow: {
        'sm':    '0 1px 2px rgba(0,0,0,0.05)',
        DEFAULT: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)',
        'md':    '0 4px 6px rgba(0,0,0,0.07)',
        'lg':    '0 10px 15px rgba(0,0,0,0.08)',
      },

      // ── Transiciones ───────────────────────────────────────────────
      transitionDuration: {
        'fast':   '100ms',  // hover de palabras en el reader
        DEFAULT:  '150ms',
        'medium': '200ms',  // apertura de popovers
        'slow':   '300ms',  // modals, transiciones de página
      },
    },
  },
  plugins: [],
};
