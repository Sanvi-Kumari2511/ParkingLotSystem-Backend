import xml.etree.ElementTree as ET
try:
    tree = ET.parse('target/site/jacoco/jacoco.xml')
    root = tree.getroot()
    for package in root.findall('package'):
        if package.get('name') == 'com/parkease/payment/service':
            for cls in package.findall('class'):
                if cls.get('name') == 'com/parkease/payment/service/PaymentServiceImpl':
                    for method in cls.findall('method'):
                        missed = sum(int(counter.get('missed')) for counter in method.findall('counter') if counter.get('type') == 'INSTRUCTION')
                        covered = sum(int(counter.get('covered')) for counter in method.findall('counter') if counter.get('type') == 'INSTRUCTION')
                        if missed > 0:
                            print(f"{method.get('name')}: missed {missed}/{missed+covered} instructions")
except Exception as e:
    print(e)
