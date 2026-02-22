import ast

class QuantifierWrapper(ast.NodeTransformer):
    def visit_Call(self, node: ast.Call):
        # We are looking for Forall or Exists calls
        if isinstance(node.func, ast.Name) and node.func.id in ["Forall", "Exists"]:
            # Nagini quantifiers usually have a lambda as one of the arguments
            # Forall(type, lambda x: body) or Forall(type, lambda x: body, triggers)
            for i, arg in enumerate(node.args):
                if isinstance(arg, ast.Lambda):
                    # We want to ensure the lambda body is wrapped in a tuple if it's not already
                    # Actually, Nagini needs (body, [[trigger]]) if triggers are present.
                    # But if we just want to ensure it's syntactically valid in Nagini's special way:
                    # The issue description says: wrap inner expression of Forall(int, lambda i: inner_expression)
                    # so to parse inner_expression until closing brace of Forall and to wrap it into braces
                    # (this inner_expression can be also like `expression, [[trigger]]` - so I need to get `(expression, [[trigger]])`
                    
                    # If it's a tuple, it will be unparsed as (a, b) which is what's needed.
                    # If it's already a tuple, we don't need to do much, but ast.Lambda.body is a single expression.
                    # If there are multiple expressions in lambda body (not possible in Python lambda), 
                    # but Nagini might have something that LOOKS like it.
                    # Actually, if the input is `lambda i: expression, [[trigger]]`, Python parser will see it as:
                    # a Tuple(Lambda(i, expression), [[trigger]]) if it's outside.
                    # But the user said: Forall(int, lambda i: expression, [[trigger]])
                    # In this case, `int`, `lambda i: expression`, and `[[trigger]]` are arguments to Forall.
                    # WAIT. If it's `Forall(int, lambda i: expression, [[trigger]])`, then `[[trigger]]` is NOT inside the lambda.
                    # But Nagini wants `Forall(int, lambda i: (expression, [[trigger]]))`.
                    
                    # Let's re-read: "I would like to wrap inner expression of Forall(int, lambda i: inner_expression), 
                    # so to parse inner_expression until closing brace of Forall and to wrap it into braces"
                    # "this inner_expression can be also like `expression, [[trigger]]` - so I need to get `(expression, [[trigger]])`"
                    
                    # If the input code is: `Forall(int, lambda i: expression, [[trigger]])`
                    # Python's `ast.parse` might FAIL if `[[trigger]]` is not valid Python or if it's misplaced.
                    # Actually, `[[trigger]]` is valid Python (list of list).
                    
                    # If the input is `Forall(int, lambda i: expression, [[trigger]])`, 
                    # `ast.Call` has 3 args: `Name(int)`, `Lambda(...)`, `List(List(...))`.
                    # We want to transform it to `Forall(int, lambda i: (expression, [[trigger]]))`.
                    # So we move subsequent arguments into the lambda body if they look like triggers.
                    
                    # If there are multiple arguments after the lambda, they might be triggers.
                    # Nagini: Forall(type, lambda x: body, [[t1]], [[t2]])
                    # We want: Forall(type, lambda x: (body, [[t1]], [[t2]]))
                    
                    triggers = []
                    while i + 1 < len(node.args) and isinstance(node.args[i+1], ast.List):
                        triggers.append(node.args.pop(i+1))
                    
                    if triggers:
                        # Move triggers into a tuple with the current lambda body
                        new_body = ast.Tuple(elts=[arg.body] + triggers, ctx=ast.Load())
                        arg.body = new_body
                    
                    # Even if there's no trigger, the user seems to want to wrap the body in parentheses.
                    # `ast.unparse` will only add parentheses if necessary for precedence.
                    # But if we want FORCE parentheses, we might need to wrap it in a Tuple with one element,
                    # but `(expr,)` is a tuple. `(expr)` is just expr.
                    # Nagini might specifically want the parentheses.
                    pass

        return self.generic_visit(node)

def fix_syntax_errors_nagini(code: str) -> str:
    # Pre-process: replace 'implies' with '<<implies>>' to make it a valid identifier for AST parsing
    placeholder = "__implies_placeholder__"
    # Use regex to replace only whole word 'implies'
    import re
    temp_code = re.sub(r'\bimplies\b', placeholder, code)
    try:
        tree = ast.parse(temp_code)
        tree = QuantifierWrapper().visit(tree)
        fixed_code = ast.unparse(tree)
        # Post-process: restore 'implies'
        return fixed_code.replace(placeholder, "implies")
    except Exception:
        # If parsing fails, return original code
        return code
