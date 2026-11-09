"""Offline regression: an exited root PID must never adopt a reused identity."""
import ast
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock


class CleanupTest(unittest.TestCase):
    def test_reused_root_and_unrelated_children_are_never_owned_or_signalled(self):
        tree = ast.parse(Path(__file__).with_name('run.py').read_text())
        observe = next(node for node in tree.body
                       if isinstance(node, ast.FunctionDef) and node.name == 'observe')
        cleanup = next(node for node in tree.body
                       if isinstance(node, ast.Try) and node.finalbody)
        process_loop = next(node for node in cleanup.finalbody if isinstance(node, ast.For))
        previous = dict(pid=4242, starttime='OLD', processGroup=4242, parentPid=1)
        records = {(4242, 'OLD'): previous}
        current = {4242: (4242, 'NEW', 'S', 1), 4243: (4242, 'CHILD', 'S', 4242)}
        proc = SimpleNamespace(iterdir=lambda: [SimpleNamespace(name=str(pid)) for pid in current])
        finished = SimpleNamespace(pid=4242, poll=lambda: 0)
        signals = Mock()
        with tempfile.TemporaryDirectory() as folder:
            namespace = dict(Path=lambda _: proc, stat=current.get, records=records,
                             processes=[finished], out=Path(folder), json=__import__('json'),
                             os=SimpleNamespace(killpg=signals), signal=__import__('signal'))
            exec(compile(ast.Module(body=[observe], type_ignores=[]), '<observe>', 'exec'), namespace)
            namespace['observe']()
            self.assertEqual(records, {(4242, 'OLD'): previous})
            exec(compile(ast.Module(body=[process_loop], type_ignores=[]), '<cleanup>', 'exec'), namespace)
            signals.assert_not_called()


if __name__ == '__main__':
    unittest.main()
