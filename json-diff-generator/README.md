Issues:
1. If an element is common in two arrays, then this is not denoted in the diff result.

2. jsonsWithIssue3_Unresolve.json, jsonsWithIssue6_Unresolve.json, jsonsWithIssue8_Unresolve.json is unresolved because it is an array where all the elements are not of the same type. 

3. jsonsWithIssue4_Unresolved.json, jsonsWithIssue5_Unresolved.json,jsonsWithIssue7_Unresolved.json  is unresolved because none of the keys in the objects are "name" hence the code will not be able to sort the objects in the array and because of that it will fail to compare them. We need to extend the code to take any sort of keys.

4. So, the issues can be basically divided into two types:
    1. In an array, I am assuming all elements to be either string, number, array or object but all elements might not be of the same type. I need to consider that in my design
    2. Currently, the key for an array of objects is considered to be "name" by default. If there is no parameter with the value "name" in the object then a NPE is thrown. Need to fix that by understanding the key somehow or taking the key as input ?