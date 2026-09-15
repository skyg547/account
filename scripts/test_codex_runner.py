#!/usr/bin/env python3
"""
Unit tests for codex-runner.py model tiering and circuit breaker
"""

import importlib.util
import os
import unittest

SCRIPT_PATH = os.path.join(os.path.dirname(__file__), "codex-runner.py")
spec = importlib.util.spec_from_file_location("codex_runner", SCRIPT_PATH)
codex_runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(codex_runner)

determine_model_and_effort = codex_runner.determine_model_and_effort
extract_modules = codex_runner.extract_modules

class TestCodexRunner(unittest.TestCase):
    def test_default_model_sol(self):
        issue = {
            "labels": [{"name": "difficulty:medium"}, {"name": "module:master-data"}]
        }
        model, effort, diff = determine_model_and_effort(issue)
        self.assertEqual(model, "gpt-5.6-sol")
        self.assertEqual(effort, "high")
        self.assertEqual(diff, "difficulty:medium")

    def test_low_difficulty_model_sol(self):
        issue = {
            "labels": [{"name": "difficulty:low"}]
        }
        model, effort, diff = determine_model_and_effort(issue)
        self.assertEqual(model, "gpt-5.6-sol")
        self.assertEqual(effort, "high")
        self.assertEqual(diff, "difficulty:low")

    def test_high_difficulty_model_sol(self):
        issue = {
            "labels": [{"name": "difficulty:high"}]
        }
        model, effort, diff = determine_model_and_effort(issue)
        self.assertEqual(model, "gpt-5.6-sol")
        self.assertEqual(effort, "high")
        self.assertEqual(diff, "difficulty:high")

    def test_very_high_difficulty_escalates_to_astra(self):
        issue = {
            "labels": [{"name": "difficulty:very-high"}, {"name": "module:closing"}]
        }
        model, effort, diff = determine_model_and_effort(issue)
        self.assertEqual(model, "gpt-6-astra")
        self.assertEqual(effort, "xhigh")
        self.assertEqual(diff, "difficulty:very-high")

    def test_explicit_model_astra_label_escalates_to_astra(self):
        issue = {
            "labels": [{"name": "difficulty:medium"}, {"name": "model:astra"}]
        }
        model, effort, diff = determine_model_and_effort(issue)
        self.assertEqual(model, "gpt-6-astra")
        self.assertEqual(effort, "xhigh")

    def test_extract_single_module(self):
        issue = {
            "labels": [{"name": "module:deposit"}]
        }
        modules, is_cross = extract_modules(issue)
        self.assertEqual(modules, ["deposit"])
        self.assertFalse(is_cross)

    def test_extract_cross_module(self):
        issue = {
            "labels": [{"name": "module:journal-ledger"}, {"name": "module:closing"}, {"name": "module:cross-module"}]
        }
        modules, is_cross = extract_modules(issue)
        self.assertIn("journal-ledger", modules)
        self.assertIn("closing", modules)
        self.assertTrue(is_cross)

if __name__ == "__main__":
    unittest.main()
