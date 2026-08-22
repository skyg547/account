import { FlatCompat } from "@eslint/eslintrc";
import { defineConfig, globalIgnores } from "eslint/config";
import reactHooks from "eslint-plugin-react-hooks";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// TODO #536: Remove this shim after react-hooks provides set-state-in-effect.
if (!Object.hasOwn(reactHooks.rules, "set-state-in-effect")) {
  reactHooks.rules["set-state-in-effect"] = {
    meta: {
      type: "problem",
      docs: { description: "Compatibility no-op for existing disable directives" },
      schema: [],
    },
    create: () => ({}),
  };
}

const compat = new FlatCompat({ baseDirectory: __dirname });

const eslintConfig = defineConfig([
  ...compat.extends("next/core-web-vitals", "next/typescript"),
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
  {
    // TODO #536: Restore these rules to errors after the existing debt is fixed.
    rules: {
      "@typescript-eslint/no-explicit-any": "warn",
      "react/no-unescaped-entities": "warn",
    },
  },
]);

export default eslintConfig;
