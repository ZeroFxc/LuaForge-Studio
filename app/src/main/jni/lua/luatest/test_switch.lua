-- ==========================================
-- Test 1: 基础测试
-- ==========================================
local x = 1
local a = switch(x) case 1 => "one" end
print("Test 1 (base):", a)  -- 期望: one

-- ==========================================
-- Test 2: 作为函数参数
-- ==========================================
local function id(v) return v end
local b = id(switch(2) case 2 => "two" end)
print("Test 2 (func arg):", b)  -- 期望: two

-- ==========================================
-- Test 3: 多 case 分支
-- ==========================================
local y = 3
local c = switch(y) case 1 => "one" case 2 => "two" case 3 => "three" end
print("Test 3 (multi):", c)  -- 期望: three

-- ==========================================
-- Test 4: default 分支
-- ==========================================
local d = switch(99) case 1 => "one" default => "default" end
print("Test 4 (default):", d)  -- 期望: default

-- ==========================================
-- Test 5: 无匹配无 default
-- ==========================================
local e = switch(99) case 1 => "one" end
print("Test 5 (no match):", e)  -- 期望: nil

-- ==========================================
-- Test 6: 字符串 case 值
-- ==========================================
local s = "hello"
local f = switch(s) case "world" => "no" case "hello" => "yes" end
print("Test 6 (string):", f)  -- 期望: yes

-- ==========================================
-- Test 7: 表达式作为 case 值 (如 a+b)
-- ==========================================
local g = switch(10) case 5+5 => "ten" case 5+6 => "eleven" end
print("Test 7 (expr case):", g)  -- 期望: ten

-- ==========================================
-- Test 8: 一个 case 匹配多个值 (逗号分隔)
-- ==========================================
local h = switch(2) case 1, 2, 3 => "1-3" case 4, 5 => "4-5" end
print("Test 8 (multi-val case):", h)  -- 期望: 1-3

-- ==========================================
-- Test 9: 嵌套 switch 表达式
-- ==========================================
local i = switch(1)
  case 1 => switch(2) case 1 => "inner-1" case 2 => "inner-2" end
  case 2 => "outer-2"
end
print("Test 9 (nested):", i)  -- 期望: inner-2

-- ==========================================
-- Test 10: switch 作为算术表达式的一部分
-- ==========================================
local j = 100 + switch(3) case 1 => 10 case 2 => 20 case 3 => 30 end
print("Test 10 (arithmetic):", j)  -- 期望: 130

-- ==========================================
-- Test 11: switch 作为 return 值
-- ==========================================
local function getGrade(score)
  return switch(score)
    case 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100 => "A"
    case 80, 81, 82, 83, 84, 85, 86, 87, 88, 89 => "B"
    case 70, 71, 72, 73, 74, 75, 76, 77, 78, 79 => "C"
    default => "F"
  end
end
print("Test 11 (return switch):", getGrade(85))  -- 期望: B

-- ==========================================
-- Test 12: switch 作为表的值
-- ==========================================
local t = {
  name = "test",
  value = switch(3) case 1 => "first" case 2 => "second" case 3 => "third" end
}
print("Test 12 (table value):", t.value)  -- 期望: third

-- ==========================================
-- Test 13: switch 在 if 条件中或不常见位置
-- ==========================================
local result = "start -> " .. switch(1) case 1 => "passed" case 2 => "failed" end .. " <- end"
print("Test 13 (concat):", result)  -- 期望: start -> passed <- end

-- ==========================================
-- Test 14: switch 控制值是函数调用结果
-- ==========================================
local function getValue() return 42 end
local k = switch(getValue()) case 42 => "forty-two" default => "other" end
print("Test 14 (func ctrl):", k)  -- 期望: forty-two

-- ==========================================
-- Test 15: 布尔 case 值
-- ==========================================
local l = switch(true) case true => "yes" case false => "no" end
print("Test 15 (bool true):", l)  -- 期望: yes

local m = switch(false) case true => "yes" case false => "no" end
print("Test 16 (bool false):", m)  -- 期望: no

-- ==========================================
-- Test 17: nil 控制值 (无匹配)
-- ==========================================
local n = switch(nil) case 1 => "one" default => "default-nil" end
print("Test 17 (nil ctrl):", n)  -- 期望: default-nil