"""
A script to scan through the versions directory and collect all folder names as the subproject list,
then output a json as the github action include matrix
"""
__author__ = 'Fallen_Breath'

import json
import os
import sys


def main():
	target_subproject_env = os.environ.get('TARGET_SUBPROJECT', '')
	target_subprojects = list(filter(None, target_subproject_env.split(',') if target_subproject_env != '' else []))
	print('target_subprojects: {}'.format(target_subprojects))

	with open('settings.json') as f:
		settings: dict = json.load(f)

	if len(target_subprojects) == 0:
		subprojects = settings['versions']
	else:
		subprojects = []
		for subproject in settings['versions']:
			if subproject in target_subprojects:
				subprojects.append(subproject)
				target_subprojects.remove(subproject)
		if len(target_subprojects) > 0:
			print('Unexpected subprojects: {}'.format(target_subprojects), file=sys.stderr)
			sys.exit(1)

	matrix_entries = []
	for subproject in subprojects:
		matrix_entries.append({
			'subproject': subproject,
		})
	matrix = {'include': matrix_entries}
	with open(os.environ['GITHUB_OUTPUT'], 'w') as f:
		f.write('matrix={}\n'.format(json.dumps(matrix)))

	print('matrix:')
	print(json.dumps(matrix, indent=2))


if __name__ == '__main__':
	main()
